package com.sbancuz.plannh.data.serialization;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nonnull;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.sbancuz.plannh.api.RecipePropertyAPI;
import com.sbancuz.plannh.data.flowchart.Edge;
import com.sbancuz.plannh.data.flowchart.Graph;
import com.sbancuz.plannh.data.flowchart.GraphData;
import com.sbancuz.plannh.data.flowchart.Group;
import com.sbancuz.plannh.data.flowchart.MachineGroup;
import com.sbancuz.plannh.data.flowchart.Node;
import com.sbancuz.plannh.data.flowchart.Note;
import com.sbancuz.plannh.data.flowchart.Port;

import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.Recipe;
import codechicken.nei.recipe.RecipeHandlerRef;

/** GuideNH Markdown with a readable flowchart and a lossless clipboard import payload. */
public final class GuideNHExporter {

    private GuideNHExporter() {}

    @Nonnull
    public static String toMarkdown(final Graph graph) {
        final StringBuilder out = new StringBuilder();
        final StringBuilder content = new StringBuilder();
        final Map<UUID, Node> nodes = new HashMap<>();

        // GuideNH reads copyValue as raw text, not a YAML string.
        out.append("---\nlayout: elk\n");
        out.append("copyValue: " + Serializer.encodeGraph(graph) + "\n");
        out.append("---\n<Mermaid>\nflowchart LR\n");

        // Nodes, notes and (recursively) groups; native recipe embeds collect in content
        appendData(out, content, nodes, graph.getNodes());
        appendData(out, content, nodes, graph.getNotes());
        appendData(out, content, nodes, graph.getGroups());

        // One labeled connection per edge, dashed when it carries nothing
        final Map<UUID, Edge> edges = graph.getEdges();
        for (final Edge edge : edges.values()) {
            final Node source = nodes.get(edge.getSourceNodeId());
            final Node target = nodes.get(edge.getTargetNodeId());
            if (source == null || target == null || edge.invalid()) continue;

            // Label with the resource from either end, preferring the producer's name
            final Port<?> output = portAt(source.getOutputs(), edge.getSourceOutputItemIndex());
            final Port<?> input = portAt(target.getInputs(), edge.getTargetInputItemIndex());
            final Port<?> port = output != null ? output : input;
            final String resource = port != null ? port.getDisplayName() : "Connection";

            if (edge.isActive(source, target)) {
                out.append("%s -->|%s| %s\n".formatted(id(source), label(resource), id(target)));
            } else {
                out.append("%s -.->|%s| %s\n".formatted(id(source), label("Inactive: " + resource), id(target)));
            }
        }

        // NodeContent blocks must follow the diagram source inside the Mermaid tag
        out.append(content);
        out.append("</Mermaid>\n");
        return out.toString();
    }

    private static void appendData(final StringBuilder out, final StringBuilder content, final Map<UUID, Node> nodes,
        final Map<UUID, ? extends GraphData> entries) {
        for (final GraphData data : entries.values()) {
            // Groups become subgraphs wrapping their children
            if (data instanceof Group group) {
                String header = group.getHeader();
                if (group instanceof MachineGroup machines && machines.getMachineCapacity() > 0) {
                    header += " (capacity: " + machines.getMachineCapacity() + " machines)";
                }
                out.append("subgraph %s[\"%s\"]\n".formatted(id(group), label(header)));
                appendData(out, content, nodes, group.getChildren());
                out.append("end\n");
                continue;
            }

            // Header first, then the machine name when it adds information
            final StringBuilder text = new StringBuilder(data.getHeader());
            if (data instanceof Node node) {
                nodes.put(node.getId(), node);
                final String machine = node.getMachineName();
                if (text.isEmpty()) text.append(machine);
                else if (!machine.isEmpty() && !machine.equals(node.getHeader())) text.append("\n" + machine);

                // Nodes without a native recipe canvas fall back to a text preview
                if (!node.isAvailable()) text.append("\nRecipe unavailable");
                else if (!appendRecipe(content, node)) {
                    appendPorts(text, "Inputs (per recipe)", node.getInputs());
                    appendPorts(text, "Outputs (per recipe)", node.getOutputs());
                }
            } else if (data instanceof Note note) {
                for (final String line : note.getText()) text.append("\n" + line);
            }

            final String body = text.isEmpty() ? "Recipe" : text.toString();
            out.append("%s[\"%s\"]\n".formatted(id(data), label(body)));
        }
    }

    private static boolean appendRecipe(final StringBuilder content, final Node node) {
        final RecipeHandlerRef ref = node.getHandlerRef();
        final Recipe.RecipeId recipeId = node.getRecipeId();
        if (!node.isAvailable() || ref == null || recipeId == null || ref.recipeIndex < 0) return false;

        // The item RecipeHandlerRef's output lookup used, which is also what its recipe index is relative to
        final ItemStack result = recipeId.getResult();
        if (result == null || result.getItem() == null) return false;
        String item = Item.itemRegistry.getNameForObject(result.getItem());
        if (item == null) return false;
        // GuideNH parses modid:name[:meta[:nbt]], so NBT needs the meta even when it is 0
        if (result.getItemDamage() != 0 || result.stackTagCompound != null) item += ":" + result.getItemDamage();
        if (result.stackTagCompound != null) item += ":" + result.stackTagCompound;

        // GregTech categories share a handler class/id; the overlay identifies the actual recipe map.
        final IRecipeHandler handler = ref.handler;
        final String overlay = handler.getOverlayIdentifier();
        final String handlerId = overlay != null && !overlay.isBlank() ? overlay : handler.getHandlerId();
        final String handlerName = handler.getRecipeName();
        final String handlerAttribute;
        if (handlerId != null && !handlerId.isBlank()) handlerAttribute = "handlerId=\"" + attribute(handlerId) + "\"";
        else if (handlerName != null && !handlerName.isBlank()) {
            handlerAttribute = "handlerName=\"" + attribute(handlerName) + "\"";
        } else return false;

        // Bold title above the native recipe canvas
        final String header = node.getHeader();
        final String title = header.isBlank() ? node.getMachineName() : header;
        content.append("""

            <NodeContent id="%s">

            **%s**

            <RecipeFor id="%s" %s recipeIndex="%d" fallbackText="Recipe unavailable" />
            </NodeContent>
            """.formatted(id(node), markdownText(title), attribute(item), handlerAttribute, ref.recipeIndex));
        return true;
    }

    private static void appendPorts(final StringBuilder text, final String heading, final List<Port<?>> ports) {
        if (ports.isEmpty()) return;
        text.append("\n" + heading + ":");
        for (final Port<?> port : ports) {
            final String unit = RecipePropertyAPI.FLUID.equals(port.getType()) ? " mB " : " x ";
            text.append("\n" + port.getAmount() + unit + port.getDisplayName());
            if (port.getChance() == 1) continue;

            // Chance as a percentage with at most two decimals
            final BigDecimal percent = BigDecimal.valueOf(port.getChance() * 100d);
            final BigDecimal rounded = percent.setScale(2, RoundingMode.HALF_UP);
            final BigDecimal trimmed = rounded.stripTrailingZeros();
            text.append(" (" + trimmed.toPlainString() + "% chance)");
        }
    }

    private static Port<?> portAt(final List<Port<?>> ports, final int index) {
        return index >= 0 && index < ports.size() ? ports.get(index) : null;
    }

    /** The node's UUID without dashes; Mermaid ids cannot start with a digit. */
    private static String id(final GraphData data) {
        final UUID id = data.getId();
        return "n%016x%016x".formatted(id.getMostSignificantBits(), id.getLeastSignificantBits());
    }

    private static String attribute(final String value) {
        return value.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("\r", "&#13;")
            .replace("\n", "&#10;");
    }

    private static String markdownText(final String text) {
        final StringBuilder out = new StringBuilder();
        for (final char c : text.toCharArray()) {
            if ("\\`*_{}[]()#+-.!|<>~".indexOf(c) >= 0) out.append('\\');
            if (c == '&') out.append("&amp;");
            else out.append(c == '\n' || c == '\r' ? ' ' : c);
        }
        return out.toString();
    }

    private static String label(final String text) {
        // GuideNH recognizes operators/comments even inside quotes and only decodes these HTML entities.
        // Normalize unsupported punctuation in the preview; copyValue retains the original text exactly.
        return text.replaceAll("§[0-9A-FK-ORa-fk-or]", "")
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace('"', '\'')
            .replace('`', '\'')
            .replace('|', '/')
            .replace('[', '(')
            .replace(']', ')')
            .replace('{', '(')
            .replace('}', ')')
            .replaceAll("%(?=%)|-(?=[-.])|=(?==)|~(?=~)|:(?=::)", "$0 ")
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .replace("\n", "<br/>");
    }
}
