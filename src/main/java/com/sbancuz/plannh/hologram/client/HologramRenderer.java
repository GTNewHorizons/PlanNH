package com.sbancuz.plannh.hologram.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.client.event.RenderWorldLastEvent;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import com.sbancuz.plannh.hologram.Hologram;
import com.sbancuz.plannh.hologram.HologramStore;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/** Draws holograms as nameplate-style billboards after the world has been rendered. */
public class HologramRenderer {

    public static final double RENDER_DISTANCE = 64.0D;

    private static final float TEXT_SCALE = 0.025F;
    private static final int PADDING = 2;
    private static final float BACKGROUND_ALPHA = 0.5F;

    @SubscribeEvent
    public void onRenderWorldLast(final RenderWorldLastEvent event) {
        final Minecraft mc = Minecraft.getMinecraft();
        final EntityLivingBase camera = mc.renderViewEntity;
        if (mc.thePlayer == null || camera == null || mc.gameSettings.hideGUI) return;
        final HologramStore store = HologramStore.current();
        if (store == null) return;

        final double cameraX = camera.lastTickPosX + (camera.posX - camera.lastTickPosX) * event.partialTicks;
        final double cameraY = camera.lastTickPosY + (camera.posY - camera.lastTickPosY) * event.partialTicks;
        final double cameraZ = camera.lastTickPosZ + (camera.posZ - camera.lastTickPosZ) * event.partialTicks;

        for (final Hologram hologram : store.inDimension(mc.thePlayer.dimension)) {
            if (hologram.text()
                .isEmpty()) continue;
            final double dx = hologram.x() + 0.5D - cameraX;
            final double dy = hologram.y() + 0.5D - cameraY;
            final double dz = hologram.z() + 0.5D - cameraZ;
            if (dx * dx + dy * dy + dz * dz > RENDER_DISTANCE * RENDER_DISTANCE) continue;
            draw(mc.fontRenderer, hologram, dx, dy, dz);
        }
    }

    private static void draw(final FontRenderer font, final Hologram hologram, final double x, final double y,
        final double z) {
        final int previousMatrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        final int previousTextureUnit = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        final float previousLightX = OpenGlHelper.lastBrightnessX;
        final float previousLightY = OpenGlHelper.lastBrightnessY;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        try {
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_FOG);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.001F);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            GL11.glDepthMask(false);

            GL11.glTranslated(x, y, z);
            GL11.glRotatef(-RenderManager.instance.playerViewY, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(RenderManager.instance.playerViewX, 1.0F, 0.0F, 0.0F);
            GL11.glScalef(-TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);

            final int width = font.getStringWidth(hologram.text());
            final int left = -width / 2 - PADDING;
            final int right = width - width / 2 + PADDING;
            final int top = -PADDING;
            final int bottom = font.FONT_HEIGHT + PADDING - 1;

            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glColor4f(0.0F, 0.0F, 0.0F, BACKGROUND_ALPHA);
            final Tessellator tessellator = Tessellator.instance;
            tessellator.startDrawingQuads();
            tessellator.addVertex(left, bottom, 0.0D);
            tessellator.addVertex(right, bottom, 0.0D);
            tessellator.addVertex(right, top, 0.0D);
            tessellator.addVertex(left, top, 0.0D);
            tessellator.draw();

            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            font.drawString(hologram.text(), -width / 2, 0, 0xFF000000 | hologram.color());
        } finally {
            // glPopAttrib alone does not restore OpenGlHelper's cached brightness or the matrix stack.
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, previousLightX, previousLightY);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopMatrix();
            GL11.glPopAttrib();
            OpenGlHelper.setActiveTexture(previousTextureUnit);
            GL11.glMatrixMode(previousMatrixMode);
        }
    }
}
