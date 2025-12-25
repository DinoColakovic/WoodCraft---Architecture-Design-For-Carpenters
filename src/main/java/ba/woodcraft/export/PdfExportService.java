package ba.woodcraft.export;

import java.io.File;
import java.io.IOException;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.CubicCurve;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.Line;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.shape.QuadCurveTo;
import javafx.scene.shape.Rectangle;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.util.Matrix;

public class PdfExportService implements ExportService {

    @Override
    public void export(CanvasDocument document, File target) throws IOException {
        double width = document.getWidth();
        double height = document.getHeight();

        try (PDDocument pdfDocument = new PDDocument()) {
            PDPage page = new PDPage(new PDRectangle((float) width, (float) height));
            pdfDocument.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(pdfDocument, page)) {
                content.transform(new Matrix(1, 0, 0, -1, 0, (float) height));
                for (Node node : document.getExportableNodes()) {
                    drawNodeToPdf(content, node);
                }
            }

            pdfDocument.save(target);
        }
    }

    private void drawNodeToPdf(PDPageContentStream content, Node node) throws IOException {
        javafx.scene.transform.Transform transform = node.getLocalToParentTransform();
        Matrix matrix = new Matrix(
                (float) transform.getMxx(),
                (float) transform.getMyx(),
                (float) transform.getMxy(),
                (float) transform.getMyy(),
                (float) transform.getTx(),
                (float) transform.getTy()
        );
        content.saveGraphicsState();
        content.transform(matrix);

        if (node instanceof Line line) {
            if (!applyStroke(content, line.getStroke(), line.getStrokeWidth())) {
                content.restoreGraphicsState();
                return;
            }
            content.moveTo((float) line.getStartX(), (float) line.getStartY());
            content.lineTo((float) line.getEndX(), (float) line.getEndY());
            content.stroke();
        } else if (node instanceof Rectangle rect) {
            boolean hasStroke = applyStroke(content, rect.getStroke(), rect.getStrokeWidth());
            boolean hasFill = applyFill(content, rect.getFill());
            content.addRect((float) rect.getX(), (float) rect.getY(), (float) rect.getWidth(), (float) rect.getHeight());
            finishFillStroke(content, hasFill, hasStroke);
        } else if (node instanceof Circle circle) {
            boolean hasStroke = applyStroke(content, circle.getStroke(), circle.getStrokeWidth());
            boolean hasFill = applyFill(content, circle.getFill());
            drawCirclePath(content, circle.getCenterX(), circle.getCenterY(), circle.getRadius());
            finishFillStroke(content, hasFill, hasStroke);
        } else if (node instanceof CubicCurve curve) {
            if (!applyStroke(content, curve.getStroke(), curve.getStrokeWidth())) {
                content.restoreGraphicsState();
                return;
            }
            content.moveTo((float) curve.getStartX(), (float) curve.getStartY());
            content.curveTo(
                    (float) curve.getControlX1(),
                    (float) curve.getControlY1(),
                    (float) curve.getControlX2(),
                    (float) curve.getControlY2(),
                    (float) curve.getEndX(),
                    (float) curve.getEndY()
            );
            content.stroke();
        } else if (node instanceof Path path) {
            boolean hasStroke = applyStroke(content, path.getStroke(), path.getStrokeWidth());
            boolean hasFill = applyFill(content, path.getFill());
            drawPath(content, path);
            finishFillStroke(content, hasFill, hasStroke);
        }

        content.restoreGraphicsState();
    }

    private boolean applyStroke(PDPageContentStream content, Paint paint, double width) throws IOException {
        java.awt.Color color = toAwtColor(paint);
        if (color == null) {
            return false;
        }
        content.setStrokingColor(color);
        content.setLineWidth((float) width);
        return true;
    }

    private boolean applyFill(PDPageContentStream content, Paint paint) throws IOException {
        java.awt.Color color = toAwtColor(paint);
        if (color == null) {
            return false;
        }
        content.setNonStrokingColor(color);
        return true;
    }

    private java.awt.Color toAwtColor(Paint paint) {
        if (!(paint instanceof Color color)) {
            return null;
        }
        if (color.getOpacity() == 0) {
            return null;
        }
        return new java.awt.Color((float) color.getRed(), (float) color.getGreen(), (float) color.getBlue(), (float) color.getOpacity());
    }

    private void finishFillStroke(PDPageContentStream content, boolean fill, boolean stroke) throws IOException {
        if (fill && stroke) {
            content.fillAndStroke();
        } else if (fill) {
            content.fill();
        } else if (stroke) {
            content.stroke();
        }
    }

    private void drawCirclePath(PDPageContentStream content, double cx, double cy, double r) throws IOException {
        double k = 0.552284749831;
        double c = r * k;
        content.moveTo((float) (cx + r), (float) cy);
        content.curveTo((float) (cx + r), (float) (cy + c), (float) (cx + c), (float) (cy + r), (float) cx, (float) (cy + r));
        content.curveTo((float) (cx - c), (float) (cy + r), (float) (cx - r), (float) (cy + c), (float) (cx - r), (float) cy);
        content.curveTo((float) (cx - r), (float) (cy - c), (float) (cx - c), (float) (cy - r), (float) cx, (float) (cy - r));
        content.curveTo((float) (cx + c), (float) (cy - r), (float) (cx + r), (float) (cy - c), (float) (cx + r), (float) cy);
        content.closePath();
    }

    private void drawPath(PDPageContentStream content, Path path) throws IOException {
        double currentX = 0;
        double currentY = 0;
        for (PathElement element : path.getElements()) {
            if (element instanceof MoveTo moveTo) {
                currentX = moveTo.getX();
                currentY = moveTo.getY();
                content.moveTo((float) currentX, (float) currentY);
            } else if (element instanceof LineTo lineTo) {
                currentX = lineTo.getX();
                currentY = lineTo.getY();
                content.lineTo((float) currentX, (float) currentY);
            } else if (element instanceof CubicCurveTo curveTo) {
                currentX = curveTo.getX();
                currentY = curveTo.getY();
                content.curveTo(
                        (float) curveTo.getControlX1(),
                        (float) curveTo.getControlY1(),
                        (float) curveTo.getControlX2(),
                        (float) curveTo.getControlY2(),
                        (float) currentX,
                        (float) currentY
                );
            } else if (element instanceof QuadCurveTo quadTo) {
                double c1x = currentX + (2.0 / 3.0) * (quadTo.getControlX() - currentX);
                double c1y = currentY + (2.0 / 3.0) * (quadTo.getControlY() - currentY);
                double c2x = quadTo.getX() + (2.0 / 3.0) * (quadTo.getControlX() - quadTo.getX());
                double c2y = quadTo.getY() + (2.0 / 3.0) * (quadTo.getControlY() - quadTo.getY());
                currentX = quadTo.getX();
                currentY = quadTo.getY();
                content.curveTo((float) c1x, (float) c1y, (float) c2x, (float) c2y, (float) currentX, (float) currentY);
            } else if (element instanceof ClosePath) {
                content.closePath();
            }
        }
    }
}
