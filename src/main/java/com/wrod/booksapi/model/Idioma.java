package com.wrod.booksapi.model;

public enum Idioma {
    ES("es"),
    EN("en"),
    FR("fr"),
    PT("pt"),
    DE("de");

    String idioma;

    private Idioma(String idioma) {
        this.idioma = idioma;
    }

    public String getIdioma() {
        return idioma;
    }

    public static Idioma fromString(String text) {
        for (Idioma idioma : Idioma.values()) {
            if (idioma.idioma.equalsIgnoreCase(text)) {
                return idioma;
            }
        }
        throw new IllegalArgumentException("No se ha encontrado nada: " + text);
    }
}
