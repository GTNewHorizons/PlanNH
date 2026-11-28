/**
 * Upgrade the JSON tree of a save to the current schema before Gson reads the tree.
 *
 * <p>
 * To change the shape of a save:
 * <ol>
 * <li>Add a {@link com.sbancuz.plannh.data.serialization.schema.SchemaStep} at the end of the steps of
 * {@link com.sbancuz.plannh.data.serialization.schema.Schema}. NEVER edit, remove or reorder a step of a released
 * build: saves of that version exist.</li>
 * <li>In a step, read and write {@code JsonObject} trees alone. The model classes and the Gson adapters are for the
 * current shape alone.</li>
 * <li>Add a save of the version before the step to the test fixtures. The graphs in {@code plannh_debug.json} are JSON
 * objects.</li>
 * </ol>
 */
@ParametersAreNonnullByDefault
package com.sbancuz.plannh.data.serialization.schema;

import javax.annotation.ParametersAreNonnullByDefault;
