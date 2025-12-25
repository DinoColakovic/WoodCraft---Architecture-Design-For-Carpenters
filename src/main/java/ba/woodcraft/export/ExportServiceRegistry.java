package ba.woodcraft.export;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

public class ExportServiceRegistry {

    private final Map<ExportFormat, ExportService> services = new EnumMap<>(ExportFormat.class);

    public ExportServiceRegistry() {
        register(ExportFormat.PDF, new PdfExportService());
    }

    public void register(ExportFormat format, ExportService service) {
        services.put(format, service);
    }

    public ExportService getService(ExportFormat format) {
        ExportService service = services.get(format);
        if (service == null) {
            throw new IllegalStateException("No export service registered for " + format);
        }
        return service;
    }

    public void export(ExportFormat format, CanvasDocument document, File target) throws IOException {
        getService(format).export(document, target);
    }
}
