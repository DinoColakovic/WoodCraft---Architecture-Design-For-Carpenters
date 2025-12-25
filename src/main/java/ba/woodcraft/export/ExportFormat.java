package ba.woodcraft.export;

public enum ExportFormat {
    PDF("PDF Files", "*.pdf");

    private final String description;
    private final String extensionPattern;

    ExportFormat(String description, String extensionPattern) {
        this.description = description;
        this.extensionPattern = extensionPattern;
    }

    public String getDescription() {
        return description;
    }

    public String getExtensionPattern() {
        return extensionPattern;
    }
}
