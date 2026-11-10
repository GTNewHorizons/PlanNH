package com.sbancuz.plannh.gui.note;

import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.TextFieldTheme;
import com.cleanroommc.modularui.utils.Alignment;
import com.sbancuz.plannh.data.flowchart.Note;
import com.sbancuz.plannh.gui.common.FlowchartTextFieldWidget;
import com.sbancuz.plannh.gui.theme.PlannhWidgetThemes;

public class NoteTextWidget extends FlowchartTextFieldWidget {

    protected static final int NOTE_MIN_W = 140;
    protected static final int NOTE_MIN_H = 60;

    public NoteTextWidget(NoteWidget parent) {
        super(parent);
        name("note.text");
        widgetTheme(PlannhWidgetThemes.NOTE_BODY);

        Note note = parent.getData();
        // add loaded note text if not empty, initialize it otherwise
        if (note.getText()
            .isEmpty())
            handler.getText()
                .add("");
        else handler.getText()
            .addAll(note.getText());

        // set note text to handler through reference
        note.setText(handler.getText());

        handler.setMaxLines(1000);
        setTextAlignment(Alignment.TopLeft);
        padding(4);

        resize(
            renderer.getMaxWidth(handler.getText()),
            (int) renderer.getFontHeight() * handler.getText()
                .size());
    }

    @Override
    protected void setupDrawText(ModularGuiContext context, TextFieldTheme widgetTheme) {
        super.setupDrawText(context, widgetTheme);
        renderer.setAlignment(this.textAlignment, -1, -1);
    }

    @Override
    protected void onTextChanged() {
        super.onTextChanged();
        resize((int) renderer.getLastActualWidth(), (int) renderer.getLastActualHeight());
    }

    protected void resize(int w, int h) {
        size(Math.max(w + 8, NOTE_MIN_W), Math.max(h + 8, NOTE_MIN_H));
    }
}
