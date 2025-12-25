package ba.woodcraft.export;

import java.io.File;
import java.io.IOException;

public interface ExportService {
    void export(CanvasDocument document, File target) throws IOException;
}
