package com.fawry.demo.todo;

import java.util.List;

/** Turns todos into CSV text that a spreadsheet can open. */
public final class TodoCsvExporter {

    private TodoCsvExporter() {
    }

    public static String toCsv(List<Todo> todos) {
        StringBuilder csv = new StringBuilder("id,title\n");
        for (Todo todo : todos) {
            csv.append(todo.id()).append(',').append(quote(todo.title())).append('\n');
        }
        return csv.toString();
    }

    private static String quote(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
