package ua.foxminded.university.web.lesson.export.pdf;

import static ua.foxminded.university.web.lesson.export.pdf.SchedulePdfStyles.ACCENT_COLOR;
import static ua.foxminded.university.web.lesson.export.pdf.SchedulePdfStyles.HEADER_LOGO_COLUMN;
import static ua.foxminded.university.web.lesson.export.pdf.SchedulePdfStyles.HEADER_TEXT_COLUMN;
import static ua.foxminded.university.web.lesson.export.pdf.SchedulePdfStyles.LOGO_PATH;
import static ua.foxminded.university.web.lesson.export.pdf.SchedulePdfStyles.LOGO_SIZE;
import static ua.foxminded.university.web.lesson.export.pdf.SchedulePdfStyles.MUTED_COLOR;
import static ua.foxminded.university.web.lesson.export.pdf.SchedulePdfStyles.TEXT_COLOR;
import static ua.foxminded.university.web.lesson.export.pdf.SchedulePdfStyles.WEEK_DATE_FORMAT;
import static ua.foxminded.university.web.lesson.export.pdf.SchedulePdfStyles.font;

import java.io.IOException;

import org.openpdf.text.Chunk;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.Image;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import ua.foxminded.university.web.lesson.export.ScheduleExportData;
import ua.foxminded.university.web.lesson.page.WeeklySchedulePageModel;

@Component
public class SchedulePdfHeaderWriter {

    public void write(
            Document document,
            ScheduleExportData data)
                    throws DocumentException, IOException {

        addHeader(document);
        addOwnerInfo(document, data);
        addWeekPeriod(document, data.schedule());
    }

    private void addHeader(Document document) 
            throws DocumentException, IOException {

        var header = new PdfPTable(2);

        header.setWidthPercentage(100);
        header.setWidths(new float[] { 
                HEADER_LOGO_COLUMN,
                HEADER_TEXT_COLUMN });

        header.addCell(logoCell());
        header.addCell(brandCell());

        document.add(header);
    }

    private PdfPCell logoCell() throws IOException {

        var resource = new ClassPathResource(LOGO_PATH);

        var logo = Image.getInstance(resource.getInputStream().readAllBytes());
        logo.scaleToFit(LOGO_SIZE, LOGO_SIZE);

        var cell = new PdfPCell();

        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(0);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.addElement(logo);

        return cell;
    }

    private PdfPCell brandCell() {

        var brand = new Phrase();

        brand.add(new Chunk("O", font(20, Font.BOLD, ACCENT_COLOR)));
        brand.add(new Chunk("pen ", font(20, Font.BOLD, TEXT_COLOR)));
        brand.add(new Chunk("U", font(20, Font.BOLD, ACCENT_COLOR)));
        brand.add(new Chunk("niversity", font(20, Font.BOLD, TEXT_COLOR)));

        var cell = new PdfPCell();

        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(0);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.addElement(brand);

        var subtitle = new Paragraph(
                "WEEKLY SCHEDULE",
                font(9, Font.BOLD, MUTED_COLOR));

        subtitle.setSpacingBefore(2);

        cell.addElement(subtitle);

        return cell;
    }

    private void addOwnerInfo(
            Document document,
            ScheduleExportData data)
                    throws DocumentException {

        var owner = new Paragraph();

        owner.setSpacingBefore(18);
        owner.add(new Chunk(data.ownerType() + ": ", font(11, Font.BOLD, MUTED_COLOR)));
        owner.add(new Chunk(data.ownerName(), font(13, Font.BOLD, TEXT_COLOR)));

        document.add(owner);

        for (var detail : data.ownerDetails()) {
            var paragraph = new Paragraph(detail, font(10, Font.NORMAL, MUTED_COLOR));

            paragraph.setSpacingBefore(2);

            document.add(paragraph);
        }
    }

    private void addWeekPeriod(
            Document document,
            WeeklySchedulePageModel schedule)
                    throws DocumentException {

        var period = "Week: %s - %s".formatted(
                schedule.weekStart().format(WEEK_DATE_FORMAT),
                schedule.weekEnd().format(WEEK_DATE_FORMAT));

        var paragraph = new Paragraph(period, font(11, Font.BOLD, TEXT_COLOR));

        paragraph.setSpacingBefore(14);
        paragraph.setSpacingAfter(14);

        document.add(paragraph);
    }
}