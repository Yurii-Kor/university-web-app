package ua.foxminded.university.web.lesson.export.pdf;

import static ua.foxminded.university.web.lesson.export.pdf.SchedulePdfStyles.PAGE_MARGIN;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;

import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.PageSize;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import ua.foxminded.university.web.lesson.export.ScheduleExportData;

@Component
@RequiredArgsConstructor
public class SchedulePdfGenerator {

    private final SchedulePdfHeaderWriter headerWriter;
    private final SchedulePdfScheduleWriter scheduleWriter;

    public byte[] generate(
            ScheduleExportData data) {

        Objects.requireNonNull(
                data,
                "data must not be null");

        var output = new ByteArrayOutputStream();

        var document =
                new Document(
                        PageSize.A4.rotate(),
                        PAGE_MARGIN,
                        PAGE_MARGIN,
                        PAGE_MARGIN,
                        PAGE_MARGIN);

        try {
            PdfWriter.getInstance(
                    document,
                    output);

            document.addTitle("Weekly schedule");
            document.addAuthor("Open University");
            document.open();

            headerWriter.write(document, data);
            scheduleWriter.write(document, data.schedule());

        } catch (DocumentException | IOException ex) {
            throw new IllegalStateException(
                    "Failed to generate schedule PDF.",
                    ex);

        } finally {
            if (document.isOpen()) document.close();
        }

        return output.toByteArray();
    }
}