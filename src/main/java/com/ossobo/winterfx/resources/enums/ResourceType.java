package com.ossobo.winterfx.resources.enums;


/**
 * ResourceType v1.0
 * Classifica o tipo do recurso.
 */
public enum ResourceType {
    FXML("fxml"),
    ALERT(".fxml"),
    CSS("css"),
    IMAGE("png", "jpg", "jpeg", "gif", "bmp", "ico"),
    ICON("png", "ico"),
    SOUND("wav", "mp3", "aiff", "aif"),
    PROPERTIES("properties", "props", "conf", "config"),
    JSON("json"),
    XML("xml"),
    TEXT("txt", "text", "md"),
    BINARY(),
    UNKNOWN("");

    private final String[] extensions;

    ResourceType(String... extensions) { this.extensions = extensions; }

    public String[] getExtensions() { return extensions.clone(); }

    public static ResourceType fromPath(String path) {
        if (path == null) return UNKNOWN;
        String lower = path.toLowerCase();
        for (ResourceType type : values()) {
            for (String ext : type.extensions) {
                if (lower.endsWith(ext)) return type;
            }
        }
        return UNKNOWN;
    }
}
