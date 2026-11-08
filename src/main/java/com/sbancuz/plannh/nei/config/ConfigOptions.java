package com.sbancuz.plannh.nei.config;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import codechicken.nei.config.OptionIntegerField;
import codechicken.nei.config.OptionTextField;

/** NEI expects each tooltip line as a separate list entry, including localized line breaks. */
final class ConfigOptions {

    private ConfigOptions() {}

    static List<String> splitLines(final List<String> tooltip) {
        return tooltip.stream()
            .flatMap(line -> Arrays.stream(line.split("\\\\n|\\R")))
            .collect(Collectors.toList());
    }

    static class IntegerField extends OptionIntegerField {

        IntegerField(final String name, final long min, final long max) {
            super(name, min, max);
        }

        @Override
        public List<String> handleTooltip(final int mousex, final int mousey, final List<String> tooltip) {
            return splitLines(super.handleTooltip(mousex, mousey, tooltip));
        }
    }

    static class TextField extends OptionTextField {

        TextField(final String name) {
            super(name);
        }

        @Override
        public List<String> handleTooltip(final int mousex, final int mousey, final List<String> tooltip) {
            return splitLines(super.handleTooltip(mousex, mousey, tooltip));
        }
    }
}
