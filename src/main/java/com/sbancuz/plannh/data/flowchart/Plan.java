package com.sbancuz.plannh.data.flowchart;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.sbancuz.plannh.PlanNH;
import com.sbancuz.plannh.api.PlanAPI;
import com.sbancuz.plannh.data.serialization.Serializer;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Plan {

    @Nullable
    private static Plan INSTANCE;

    private transient final List<Graph> graphs = new ArrayList<>();
    private transient String persistencePath;
    private int activeIndex = 0;

    @Getter
    @Setter
    private Summary summary = new Summary();

    private Plan() {}

    public static Plan getInstance() {
        if (INSTANCE == null) {
            INSTANCE = loadPlan();
        }
        return INSTANCE;
    }

    public static Graph getActiveGraph() {
        Plan plan = getInstance();
        if (plan.graphs.isEmpty()) {
            plan.graphs.add(new Graph("Slot 1"));
        }
        if (plan.activeIndex < 0 || plan.activeIndex >= plan.graphs.size()) {
            plan.activeIndex = 0;
        }
        return plan.graphs.get(plan.activeIndex);
    }

    private static Plan loadPlan() {
        PlanAPI.beginLoad();
        final File saveFile;
        try {
            saveFile = PlanAPI.getSaveFile();
        } catch (final Exception | LinkageError e) {
            PlanNH.LOG.error("Failed to locate PlanNH save file", e);
            PlanAPI.showMessage("plannh.error.load");
            return newPlan(null);
        }

        try {
            if (saveFile.isFile()) {
                final String data = Files.readString(saveFile.toPath(), StandardCharsets.UTF_8);
                final Plan plan = Serializer.decodePlan(data);
                if (plan != null) return loadedPlan(plan, saveFile);
                PlanAPI.preserveUnreadablePrimary(saveFile);
                PlanAPI.showMessage("plannh.error.load");
            }
        } catch (final Exception | LinkageError e) {
            PlanAPI.preserveUnreadablePrimary(saveFile);
            PlanNH.LOG.error("Failed to load primary PlanNH save", e);
            PlanAPI.showMessage("plannh.error.load");
        }

        final File backupFile = PlanAPI.getBackupSaveFile(saveFile);
        try {
            if (backupFile.isFile()) {
                final String data = Files.readString(backupFile.toPath(), StandardCharsets.UTF_8);
                final Plan plan = Serializer.decodePlan(data);
                if (plan != null) {
                    PlanAPI.showMessage("plannh.warning.backup_loaded");
                    return loadedPlan(plan, saveFile);
                }
            }
        } catch (final Exception | LinkageError e) {
            PlanNH.LOG.error("Failed to load PlanNH backup save", e);
            PlanAPI.showMessage("plannh.error.load_backup");
        }

        return newPlan(saveFile);
    }

    private static Plan loadedPlan(final Plan plan, final File saveFile) {
        plan.bindSavePath(saveFile);
        return plan;
    }

    private static Plan newPlan(final File saveFile) {
        final Plan plan = new Plan();
        plan.getGraphs()
            .add(new Graph());
        if (saveFile != null) plan.bindSavePath(saveFile);
        return plan;
    }

    public void bindSavePath(final File saveFile) {
        persistencePath = saveFile == null ? null : saveFile.getAbsolutePath();
    }

    public static void unloadPlan() {
        if (INSTANCE != null) PlanAPI.save();
        INSTANCE = null;
    }
}
