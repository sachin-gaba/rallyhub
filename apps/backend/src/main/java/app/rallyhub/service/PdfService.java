package app.rallyhub.service;

import app.rallyhub.domain.model.HealthDeclaration;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Generates a timestamped PDF for a completed health declaration.
 * Uses OpenPDF (LGPL) — GraalVM native-image compatible.
 */
@Slf4j
@Singleton
public class PdfService {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm z").withZone(ZoneOffset.UTC);

    private static final String[] PAR_Q_QUESTIONS = {
        "Has a doctor ever said you have a heart condition and should only do physical activity recommended by a doctor?",
        "Do you feel pain in your chest when you do physical activity?",
        "In the past month, have you had chest pain when NOT doing physical activity?",
        "Do you lose your balance because of dizziness, or ever lose consciousness?",
        "Do you have a bone, joint, or soft-tissue problem that could be made worse by a change in physical activity?",
        "Is your doctor currently prescribing medication for blood pressure or a heart condition?",
        "Do you know of any other reason why you should not do physical activity?"
    };

    public byte[] generateHealthDeclarationPdf(HealthDeclaration d, String memberName, String clubName) {
        try (var out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 50, 50, 60, 60);
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font titleFont   = new Font(Font.HELVETICA, 16, Font.BOLD);
            Font headingFont = new Font(Font.HELVETICA, 12, Font.BOLD);
            Font bodyFont    = new Font(Font.HELVETICA, 10, Font.NORMAL);
            Font smallFont   = new Font(Font.HELVETICA,  8, Font.ITALIC, Color.GRAY);

            // Header
            doc.add(new Paragraph("Health Declaration & Liability Waiver", titleFont));
            doc.add(new Paragraph(clubName, headingFont));
            doc.add(new Paragraph("Member: " + memberName, bodyFont));
            doc.add(new Paragraph("Submitted: " + FMT.format(d.getSubmittedAt()), bodyFont));
            doc.add(new Paragraph("Declaration ID: " + d.getId(), smallFont));
            doc.add(Chunk.NEWLINE);

            // Part 1 — PAR-Q
            doc.add(new Paragraph("Part 1 — Physical Activity Readiness (PAR-Q)", headingFont));
            PdfPTable parqTable = new PdfPTable(3);
            parqTable.setWidthPercentage(100);
            parqTable.setWidths(new float[]{0.5f, 6f, 1f});
            addTableHeader(parqTable, "#", "Question", "Answer");
            for (int i = 0; i < PAR_Q_QUESTIONS.length; i++) {
                parqTable.addCell(cell(String.valueOf(i + 1), bodyFont));
                parqTable.addCell(cell(PAR_Q_QUESTIONS[i], bodyFont));
                Boolean ans = d.getParqAnswers() != null ? d.getParqAnswers().get(i) : null;
                parqTable.addCell(cell(ans == null ? "—" : (ans ? "YES" : "NO"), bodyFont));
            }
            doc.add(parqTable);
            doc.add(Chunk.NEWLINE);

            // Part 2 — Medical conditions
            doc.add(new Paragraph("Part 2 — Medical History", headingFont));
            List<String> conditions = d.getMedicalConditions();
            if (conditions == null || conditions.isEmpty()) {
                doc.add(new Paragraph("None disclosed", bodyFont));
            } else {
                conditions.forEach(c -> { try { doc.add(new Paragraph("• " + c, bodyFont)); } catch (Exception ignored) {} });
            }
            doc.add(Chunk.NEWLINE);

            // Part 3 — Medications
            doc.add(new Paragraph("Part 3 — Current Medications", headingFont));
            doc.add(new Paragraph(
                    (d.getMedications() == null || d.getMedications().isBlank()) ? "None disclosed" : d.getMedications(),
                    bodyFont));
            doc.add(Chunk.NEWLINE);

            // Part 4 — Emergency contact
            doc.add(new Paragraph("Part 4 — Emergency Contact", headingFont));
            if (d.getEmergencyContact() != null) {
                var ec = d.getEmergencyContact();
                doc.add(new Paragraph("Name: " + ec.getFullName(), bodyFont));
                doc.add(new Paragraph("Relationship: " + ec.getRelationship(), bodyFont));
                doc.add(new Paragraph("Phone: " + ec.getPrimaryPhone(), bodyFont));
                if (ec.getSecondaryPhone() != null)
                    doc.add(new Paragraph("Secondary phone: " + ec.getSecondaryPhone(), bodyFont));
            }
            doc.add(Chunk.NEWLINE);

            // Part 5 & 6 — Consents
            doc.add(new Paragraph("Part 5 — Liability Waiver", headingFont));
            doc.add(new Paragraph("Accepted: " + (d.isLiabilityAccepted() ? "YES" : "NO"), bodyFont));
            doc.add(Chunk.NEWLINE);
            doc.add(new Paragraph("Part 6 — Data Protection Consent", headingFont));
            doc.add(new Paragraph("Consented: " + (d.isDataProtectionConsented() ? "YES" : "NO"), bodyFont));
            doc.add(Chunk.NEWLINE);

            // Footer
            doc.add(new Paragraph(
                    "This document was generated automatically by RallyHub on " + FMT.format(d.getSubmittedAt())
                    + ". Document ID: " + d.getId(), smallFont));

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("PDF generation failed for declaration {}: {}", d.getId(), e.getMessage(), e);
            throw new RuntimeException("PDF generation failed", e);
        }
    }

    private void addTableHeader(PdfPTable table, String... headers) {
        Font hf = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, hf));
            cell.setBackgroundColor(new Color(37, 99, 235)); // #2563EB
            cell.setPadding(6);
            table.addCell(cell);
        }
    }

    private PdfPCell cell(String text, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setPadding(5);
        return c;
    }
}
