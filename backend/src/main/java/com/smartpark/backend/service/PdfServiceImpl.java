package com.smartpark.backend.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.smartpark.backend.dto.PaymentDTO;
import com.smartpark.backend.dto.PersonnelDTO;
import com.smartpark.backend.dto.ContractDTO;
import com.smartpark.backend.model.CompanySettings;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfServiceImpl implements IPdfService {

    private final ISettingsService settingsService;

    @Override
    public byte[] generateFichePaie(PaymentDTO p) {
        if (p == null) {
            log.error("PaymentDTO est null");
            throw new IllegalArgumentException("Impossible de générer un PDF avec un paiement null");
        }

        CompanySettings settings = getOrDefaultSettings();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 30, 30, 30, 30);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // --- POLICES ---
            Font companyFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.BLACK);
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.BLACK);

            Color borderColor = new Color(180, 180, 180);
            Color headerBg = new Color(235, 235, 235);
            Color lightGray = new Color(250, 250, 250);

            // 1. EN-TÊTE SOCIÉTÉ
            PdfPTable headerTable = new PdfPTable(new float[]{2, 1.2f});
            headerTable.setWidthPercentage(100);

            PdfPCell leftHeader = new PdfPCell();
            leftHeader.setBorder(Rectangle.NO_BORDER);
            leftHeader.addElement(new Paragraph(settings.getCompanyName(), companyFont));
            leftHeader.addElement(new Paragraph(settings.getAddress(), normalFont));
            
            // Validation du matricule fiscal
            String mfText = (settings.getMatriculeFiscal() != null && !settings.getMatriculeFiscal().isEmpty()) 
                ? "MF: " + settings.getMatriculeFiscal()
                : "MF: Non renseigné";
            leftHeader.addElement(new Paragraph(mfText, normalFont));
            headerTable.addCell(leftHeader);

            PdfPCell rightHeader = new PdfPCell();
            rightHeader.setBorder(Rectangle.NO_BORDER);
            rightHeader.setHorizontalAlignment(Element.ALIGN_RIGHT);
            Paragraph titlePara = new Paragraph("BULLETIN DE PAIE", titleFont);
            titlePara.setAlignment(Element.ALIGN_RIGHT);
            rightHeader.addElement(titlePara);
            rightHeader.addElement(new Paragraph("Période: " + p.getMonth() + " " + p.getYear(), boldFont));
            headerTable.addCell(rightHeader);

            document.add(headerTable);
            document.add(new Chunk("\n"));

            // 2. BLOC EMPLOYÉ
            PdfPTable empTable = new PdfPTable(new float[]{1.5f, 2.5f, 1.5f, 2.5f});
            empTable.setWidthPercentage(100);

            addLabelValue(empTable, "Matricule", p.getMatricule(), boldFont, normalFont, borderColor);
            addLabelValue(empTable, "Nom & Prénom", p.getFullPersonnelName(), boldFont, normalFont, borderColor);
            addLabelValue(empTable, "C.I.N", p.getCin() != null ? p.getCin() : "N/A", boldFont, normalFont, borderColor);
            addLabelValue(empTable, "RIB", p.getRib() != null ? p.getRib() : "N/A", boldFont, normalFont, borderColor);

            document.add(empTable);

            // 3. TABLEAU RUBRIQUES DÉTAILLÉES
            PdfPTable mainTable = new PdfPTable(new float[]{3.5f, 1.2f, 1f, 1f, 1.5f, 1.5f});
            mainTable.setWidthPercentage(100);
            mainTable.setSpacingBefore(10);

            String[] colHeaders = {"Rubrique", "Base", "Taux %", "Nombre", "Gains", "Retenues"};
            for (String h : colHeaders) {
                PdfPCell hCell = new PdfPCell(new Phrase(h, headerFont));
                hCell.setBackgroundColor(headerBg);
                hCell.setPadding(5);
                hCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                mainTable.addCell(hCell);
            }

            double salaireBase = p.getSalaireBase() != null ? p.getSalaireBase() : 0;
            double salaireMensuelContrat = p.getSalaireBaseMensuel() != null ? p.getSalaireBaseMensuel() : salaireBase;
            int workingDays = (p.getWorkingDaysPerMonth() != null && p.getWorkingDaysPerMonth() > 0) ? p.getWorkingDaysPerMonth() : 22;
            long payableDays = (p.getPayableDays() != null && p.getPayableDays() > 0) ? p.getPayableDays() : workingDays;
            double dailyRate = workingDays > 0 ? (salaireMensuelContrat / workingDays) : 0;
            double totalAvantages = 0;

            // 3a. SALAIRE DE BASE
            addPayRow(mainTable, "Salaire de base", fmt(dailyRate), "", String.valueOf(payableDays), fmt(salaireBase), "", normalFont, Color.WHITE, borderColor);

            // 3b. AVANTAGES (Prime Transport, Prime Risque, Panier)
            if (p.getAvantages() != null && !p.getAvantages().isEmpty()) {
                Double primeTransport = p.getAvantages().getOrDefault("primeTransport", 0.0);
                Double primeRisque = p.getAvantages().getOrDefault("primeRisque", 0.0);
                Double panier = p.getAvantages().getOrDefault("panier", 0.0);

                if (primeTransport > 0) {
                    addPayRow(mainTable, "Prime Transport", "", "", "", fmt(primeTransport), "", normalFont, Color.WHITE, borderColor);
                    totalAvantages += primeTransport;
                }
                if (primeRisque > 0) {
                    addPayRow(mainTable, "Prime Risque", "", "", "", fmt(primeRisque), "", normalFont, Color.WHITE, borderColor);
                    totalAvantages += primeRisque;
                }
                if (panier > 0) {
                    addPayRow(mainTable, "Panier", "", "", "", fmt(panier), "", normalFont, Color.WHITE, borderColor);
                    totalAvantages += panier;
                }
            }

            // 3c. SALAIRE BRUT COTISABLE (sous-total)
            double salaireBrutCotisable = salaireBase + totalAvantages;
            addPayRow(mainTable, "SALAIRE BRUT COTISABLE", "", "", "", fmt(salaireBrutCotisable), "", boldFont, headerBg, borderColor);

            // 3d. ABSENCES (affichage informatif, inclut JUSTIFIED)
            if (p.getReferenceAbsenceDays() > 0) {
                addPayRow(
                        mainTable,
                        "Absences (" + p.getReferenceAbsenceDays() + " jours)",
                        "", "", String.valueOf(p.getReferenceAbsenceDays()), "", "",
                        normalFont,
                        lightGray,
                        borderColor
                );
            }

            // 3d-bis. DÉTAIL QUOTA (cumulé) + PÉRIODE DES ABSENCES
            if (p.getAbsencePeriodStart() != null && p.getAbsencePeriodEnd() != null) {
                addPayRow(
                        mainTable,
                        "Période absences : " + p.getAbsencePeriodStart() + " → " + p.getAbsencePeriodEnd(),
                        "", "", "", "", "",
                        normalFont,
                        lightGray,
                        borderColor
                );
            }
            if (p.getAbsenceQuotaMonthlyDays() != null || p.getAbsenceQuotaAvailableBefore() != null) {
                String monthly = p.getAbsenceQuotaMonthlyDays() != null ? String.valueOf(p.getAbsenceQuotaMonthlyDays()) : "-";
                String available = p.getAbsenceQuotaAvailableBefore() != null ? String.valueOf(p.getAbsenceQuotaAvailableBefore()) : "-";
                addPayRow(
                        mainTable,
                        "Quota absence (" + monthly + " j/mois) - Disponible avant période",
                        "", "", available, "", "",
                        normalFont,
                        lightGray,
                        borderColor
                );
            }
            if (p.getJustifiedAbsenceDays() != null) {
                addPayRow(
                        mainTable,
                        "Absences justifiées (période)",
                        "", "", String.valueOf(p.getJustifiedAbsenceDays()), "", "",
                        normalFont,
                        lightGray,
                        borderColor
                );
            }
            if (p.getNonJustifiedAbsenceDays() != null) {
                addPayRow(
                        mainTable,
                        "Absences non justifiées (période)",
                        "", "", String.valueOf(p.getNonJustifiedAbsenceDays()), "", "",
                        normalFont,
                        lightGray,
                        borderColor
                );
            }
            if (p.getPenalizedJustifiedAbsenceDays() != null && p.getPenalizedJustifiedAbsenceDays() > 0) {
                addPayRow(
                        mainTable,
                        "Dépassement justifiées pénalisé",
                        "", "", String.valueOf(p.getPenalizedJustifiedAbsenceDays()), "", "",
                        normalFont,
                        lightGray,
                        borderColor
                );
            }
            if (p.getAbsenceQuotaRemainingAfter() != null) {
                addPayRow(
                        mainTable,
                        "Quota restant après période",
                        "", "", String.valueOf(p.getAbsenceQuotaRemainingAfter()), "", "",
                        normalFont,
                        lightGray,
                        borderColor
                );
            }

            // 3e. DÉDUCTIONS ABSENCES (non justifiées seulement)
            if (p.getDeductionsAbsence() != null && p.getDeductionsAbsence() > 0) {
                addPayRow(mainTable, "Déduction Absences (" + p.getTotalAbsenceDays() + " jours)", 
                    fmt(dailyRate), "", fmt(p.getTotalAbsenceDays()), "", fmt(p.getDeductionsAbsence()), normalFont, lightGray, borderColor);
            }

            // 3f. COTISATION CNSS
            if (p.getMontantCnss() != null && p.getMontantCnss() > 0) {
                addPayRow(mainTable, "Cotisation CNSS Employé", fmt(salaireBrutCotisable), "9.18", "", "", fmt(p.getMontantCnss()), normalFont, Color.WHITE, borderColor);
            }

            // 3g. IRPP
            if (p.getMontantIrpp() != null && p.getMontantIrpp() > 0) {
                addPayRow(mainTable, "I.R.P.P", fmt(salaireBrutCotisable - (p.getDeductionsAbsence() != null ? p.getDeductionsAbsence() : 0)), "10", "", "", fmt(p.getMontantIrpp()), normalFont, lightGray, borderColor);
            }

            document.add(mainTable);

            // 4. PIED DE PAGE - TOTAUX ET NET À PAYER
            PdfPTable footerTable = new PdfPTable(new float[]{4f, 2f});
            footerTable.setWidthPercentage(100);
            footerTable.setSpacingBefore(10);

            PdfPCell payInfo = new PdfPCell();
            payInfo.setBorder(Rectangle.BOX);
            payInfo.setPadding(8);
            payInfo.addElement(new Paragraph("Mode de paiement : VIREMENT", boldFont));
            payInfo.addElement(new Paragraph("RIB : " + (p.getRib() != null ? p.getRib() : "N/A"), normalFont));
            footerTable.addCell(payInfo);

            PdfPCell netCell = new PdfPCell();
            netCell.setBackgroundColor(headerBg);
            netCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            Paragraph netP = new Paragraph("Salaire Net  ", boldFont);
            double netToPay = p.getFinalAmount() != null ? p.getFinalAmount() : 0;
            Paragraph netV = new Paragraph(fmt(netToPay) + " TND", titleFont);
            netP.setAlignment(Element.ALIGN_CENTER);
            netV.setAlignment(Element.ALIGN_CENTER);
            netCell.addElement(netP);
            netCell.addElement(netV);
            footerTable.addCell(netCell);

            document.add(footerTable);

            // 5. SIGNATURE NUMÉRIQUE (si disponible)
            addSignatureBlock(document, settings);

        } catch (Exception e) {
            log.error("Erreur lors de la génération du PDF: ", e);
            try {
                document.add(new Paragraph("Erreur technique lors de la génération: " + e.getMessage()));
            } catch (DocumentException ex) {
                log.error("Impossible d'ajouter le paragraphe d'erreur", ex);
            }
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
        return out.toByteArray();
    }

    @Override
    public byte[] generateContratPersonnel(PersonnelDTO personnel) {
        if (personnel == null) {
            throw new IllegalArgumentException("Personnel est null");
        }
        ContractDTO c = personnel.getContrat();
        if (c == null) {
            throw new IllegalStateException("Aucun contrat trouvé pour ce personnel");
        }

        CompanySettings settings = getOrDefaultSettings();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, Color.BLACK);
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.BLACK);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);

            Paragraph title = new Paragraph("CONTRAT DE TRAVAIL", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Chunk("\n"));

            document.add(new Paragraph("Entre les soussignés :", boldFont));
            document.add(new Chunk("\n"));

            document.add(new Paragraph("La société :", sectionFont));
            document.add(new Paragraph(safe(settings.getCompanyName()), normalFont));
            document.add(new Paragraph("Adresse : " + orNA(settings.getAddress()), normalFont));
            document.add(new Paragraph("Représentée par : " + "Le Responsable des Ressources Humaines ", normalFont));
            document.add(new Paragraph("Fonction : " + responsableFonction(c), normalFont));
            document.add(new Chunk("\n"));
            document.add(new Paragraph("Ci-après dénommée \"l’Employeur\"", boldFont));
            document.add(new Chunk("\n"));

            document.add(new Paragraph("ET", sectionFont));
            document.add(new Chunk("\n"));

            document.add(new Paragraph("Monsieur/Madame :", sectionFont));
            String fullName = (safe(personnel.getNom()) + " " + safe(personnel.getPrenom())).trim();
            document.add(new Paragraph(fullName.isEmpty() ? "Non renseigné" : fullName, normalFont));
            document.add(new Paragraph("Adresse : " + defaultEmployeeAddress(), normalFont));
            document.add(new Paragraph("Numéro CIN : " + orNA(personnel.getCin()), normalFont));
            document.add(new Chunk("\n"));
            document.add(new Paragraph("Ci-après dénommé(e) \"le Salarié\"", boldFont));

            document.add(new Chunk("\n"));
            addDividerLine(document);
            document.add(new Chunk("\n"));

            document.add(new Paragraph("Article 1 : Objet du contrat", sectionFont));
            {
                Phrase ph = new Phrase();
                ph.add(new Chunk(
                        "Le présent contrat a pour objet de définir les conditions dans lesquelles le salarié est recruté en qualité de ",
                        normalFont));
                ph.add(new Chunk(orNA(c.getRole()), boldFont));
                ph.add(new Chunk(".", normalFont));
                document.add(new Paragraph(ph));
            }

            document.add(new Chunk("\n"));
            addDividerLine(document);
            document.add(new Chunk("\n"));

            document.add(new Paragraph("Article 2 : Date d’effet", sectionFont));
            document.add(new Paragraph("Le présent contrat prend effet à compter du " + formatDate(c.getDateDebut()) + ".", normalFont));

            document.add(new Chunk("\n"));
            addDividerLine(document);
            document.add(new Chunk("\n"));

            document.add(new Paragraph("Article 3: Lieu de travail", sectionFont));
            document.add(new Paragraph("Le salarié exercera ses fonctions à :", normalFont));
            document.add(new Paragraph(orNA(settings.getAddress()), normalFont));

            document.add(new Chunk("\n"));
            addDividerLine(document);
            document.add(new Chunk("\n"));

            document.add(new Paragraph("Article 4 : Rémunération", sectionFont));
            double totalPrimes = 0.0;
            if (c.getAvantages() != null && !c.getAvantages().isEmpty()) {
                for (Double amount : c.getAvantages().values()) {
                    if (amount != null) totalPrimes += amount;
                }
            }
            String primes = fmtMoney(totalPrimes) + " TND";
            String salaire = (c.getSalaireBase() != null) ? (fmtMoney(c.getSalaireBase()) + " TND") : "Non renseigné";
            {
                Phrase ph = new Phrase();
                ph.add(new Chunk("En contrepartie de son travail, le salarié percevra un salaire mensuel brut de : ", normalFont));
                ph.add(new Chunk(salaire, boldFont));
                ph.add(new Chunk(", payable à la fin de chaque mois.", normalFont));
                document.add(new Chunk("\n"));
                ph.add(new Chunk(" Le salarié percevra un total des primes de : ", normalFont));
                ph.add(new Chunk(primes, boldFont));

                document.add(new Paragraph(ph));
            }

            document.add(new Chunk("\n"));
            addDividerLine(document);
            document.add(new Chunk("\n"));

            document.add(new Paragraph("Article 5 : Temps de travail", sectionFont));
            {
                Phrase ph = new Phrase();
                ph.add(new Chunk("La durée de travail est fixée à ", normalFont));
                ph.add(new Chunk("48", boldFont));
                ph.add(new Chunk(" heures par semaine, conformément à la législation en vigueur.", normalFont));
                document.add(new Paragraph(ph));
            }

            document.add(new Chunk("\n"));
            addDividerLine(document);
            document.add(new Chunk("\n"));

            document.add(new Paragraph("Article 6 : Congés", sectionFont));
            document.add(new Paragraph("Le salarié bénéficie de congés payés conformément à la législation tunisienne.", normalFont));

            document.add(new Chunk("\n"));
            addDividerLine(document);
            document.add(new Chunk("\n"));

            document.add(new Paragraph("Article 7 : Obligations du salarié", sectionFont));
            document.add(new Paragraph("Le salarié s’engage à :", normalFont));
            com.lowagie.text.List list = new com.lowagie.text.List(com.lowagie.text.List.UNORDERED);
            list.setListSymbol("• ");
            list.add(new ListItem("Respecter le règlement intérieur", normalFont));
            list.add(new ListItem("Exécuter ses tâches avec sérieux et professionnalisme", normalFont));
            list.add(new ListItem("Garder la confidentialité des informations", normalFont));
            document.add(list);

            document.add(new Chunk("\n"));
            addDividerLine(document);
            document.add(new Chunk("\n"));

            document.add(new Paragraph("Article 8 : Droit applicable", sectionFont));
            document.add(new Paragraph("Le présent contrat est soumis au droit du travail tunisien.", normalFont));


            document.add(new Chunk("\n"));
            addDividerLine(document);
            document.add(new Chunk("\n"));

            document.add(new Paragraph("Article 9 : Résiliation", sectionFont));
            document.add(new Paragraph("Le contrat peut être résilié par l’une ou l’autre des parties dans le respect des dispositions légales.", normalFont));
            
            document.add(new Chunk("\n"));
            addDividerLine(document);
            document.add(new Chunk("\n"));

            if (c.getRole() != null && (c.getRole().toLowerCase().contains("developer") || c.getRole().toLowerCase().contains("manager"))) {
                document.add(new Paragraph("Article 10 : Clause de résiliation", sectionFont));
                document.add(new Paragraph("Le salarie doit informer l'employeur par écrit au moins 15 jours avant la date prévue pour la résiliation.", normalFont));
                document.add(new Chunk("\n"));
                addDividerLine(document);
                document.add(new Chunk("\n"));
            }
            

            document.add(new Paragraph(
                "Fait à " + defaultCity() + ", le " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                normalFont));

            document.add(new Chunk("\n"));
            addContractSignaturesBlock(document, settings, normalFont, boldFont);

        } catch (Exception e) {
            log.error("Erreur génération contrat PDF", e);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }

        return out.toByteArray();
    }

    @Override
    public byte[] generateAttestationTravail(PersonnelDTO personnel) {
        if (personnel == null) {
            throw new IllegalArgumentException("Personnel est null");
        }

        CompanySettings settings = getOrDefaultSettings();
        ContractDTO c = personnel.getContrat();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 50, 50, 60, 60);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, Color.BLACK);
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.BLACK);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.BLACK);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.BLACK);

            Paragraph title = new Paragraph("ATTESTATION DE TRAVAIL", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Chunk("\n"));

            document.add(new Paragraph("Je soussigné(e),", normalFont));
            document.add(new Paragraph("Responsable des Ressources Humaines de cette entreprise", boldFont));
            document.add(new Paragraph("agissant en qualité de " + responsableFonction(c) + ",", normalFont));
            document.add(new Paragraph("au sein de la société " + safe(settings.getCompanyName()) + ",", normalFont));
            document.add(new Chunk("\n"));
            document.add(new Paragraph("atteste que :", sectionFont));
            document.add(new Chunk("\n"));

            String fullName = (safe(personnel.getNom()) + " " + safe(personnel.getPrenom())).trim();
            String cin = orNA(personnel.getCin());
            String role = (c != null) ? orNA(c.getRole()) : "Non renseigné";

            String debut = (c != null) ? formatDate(c.getDateDebut()) : "Non renseigné";
            String periode;
            if (c != null && c.getDateDebut() != null) {
                if (c.getDateFin() != null) {
                    periode = "du " + debut + " au " + formatDate(c.getDateFin());
                } else {
                    periode = "depuis le " + debut + " jusqu’à ce jour";
                }
            } else {
                periode = "Non renseigné";
            }

            document.add(new Paragraph(
                    "Monsieur/Madame " + (fullName.isEmpty() ? "Non renseigné" : fullName) + ", titulaire de la CIN n° " + cin + ",",
                    normalFont));
                {
                Phrase ph = new Phrase();
                ph.add(new Chunk("a été employé(e) dans notre société en qualité de ", normalFont));
                ph.add(new Chunk(role, boldFont));
                ph.add(new Chunk(",", normalFont));
                document.add(new Paragraph(ph));
                }
            document.add(new Chunk("\n"));
            document.add(new Paragraph(periode + ".", normalFont));
            document.add(new Chunk("\n"));
            document.add(new Paragraph("Durant cette période, l’intéressé(e) a fait preuve de sérieux, de compétence et de professionnalisme.", normalFont));
            document.add(new Chunk("\n"));
            document.add(new Paragraph("La présente attestation est délivrée à l’intéressé(e) pour servir et valoir ce que de droit.", normalFont));
            document.add(new Chunk("\n"));
            document.add(new Paragraph(
                    "Fait à " + defaultCity() + ", le " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    normalFont));

            document.add(new Chunk("\n"));
            addAttestationSignatureBlock(document, settings, normalFont);

        } catch (Exception e) {
            log.error("Erreur génération attestation PDF", e);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }

        return out.toByteArray();
    }

    private void addSignatureBlock(Document document, CompanySettings settings) {
        try {
            if (settings == null) return;
            String fileName = settings.getSignatureFileName();
            if (fileName == null || fileName.trim().isEmpty()) return;

            if (fileName.contains("/")) {
                fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
            }

            Path uploadsRoot = Paths.get(System.getProperty("user.dir")).resolve("uploads");
            Path signaturePath = uploadsRoot.resolve(fileName);
            if (!Files.exists(signaturePath)) {
                log.warn("Signature introuvable: {}", signaturePath.toAbsolutePath());
                return;
            }

            document.add(new Chunk("\n"));

            PdfPTable sigTable = new PdfPTable(new float[]{3f, 2f});
            sigTable.setWidthPercentage(100);

            PdfPCell left = new PdfPCell(new Phrase(""));
            left.setBorder(Rectangle.NO_BORDER);
            sigTable.addCell(left);

            PdfPCell right = new PdfPCell();
            right.setBorder(Rectangle.NO_BORDER);
            right.setHorizontalAlignment(Element.ALIGN_RIGHT);

            Paragraph label = new Paragraph("Signature", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9));
            label.setAlignment(Element.ALIGN_RIGHT);
            right.addElement(label);

            Image img = Image.getInstance(signaturePath.toAbsolutePath().toString());
            img.scaleToFit(140, 70);
            img.setAlignment(Image.ALIGN_RIGHT);
            right.addElement(img);

            sigTable.addCell(right);
            document.add(sigTable);
        } catch (Exception e) {
            log.warn("Impossible d'ajouter la signature au PDF: {}", e.getMessage());
        }
    }

    private void addContractSignaturesBlock(Document document, CompanySettings settings, Font normalFont, Font boldFont) {
        try {
            PdfPTable table = new PdfPTable(new float[]{1f, 1f});
            table.setWidthPercentage(100);

            PdfPCell employer = new PdfPCell();
            employer.setBorder(Rectangle.NO_BORDER);
            employer.setPadding(6);
            employer.addElement(new Paragraph("Signature de l’employeur", boldFont));
            employer.addElement(new Paragraph("(Signature)", normalFont));
            addSignatureImageToCell(employer, settings);
            table.addCell(employer);

            PdfPCell employee = new PdfPCell();
            employee.setBorder(Rectangle.NO_BORDER);
            employee.setPadding(6);
            employee.addElement(new Paragraph("Signature du salarié", boldFont));
            employee.addElement(new Paragraph("(Signature précédée de la mention \"Lu et approuvé\")", normalFont));
            employee.addElement(new Chunk("\n\n\n"));
            employee.addElement(new Paragraph("___________________________", normalFont));
            table.addCell(employee);

            document.add(table);
        } catch (Exception e) {
            log.warn("Impossible d'ajouter le bloc signatures contrat: {}", e.getMessage());
        }
    }

    private void addAttestationSignatureBlock(Document document, CompanySettings settings, Font normalFont) {
        try {
            Paragraph p = new Paragraph("Signature ", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11));
            p.setAlignment(Element.ALIGN_RIGHT);
            document.add(p);

            if (settings == null) return;
            String fileName = settings.getSignatureFileName();
            if (fileName == null || fileName.trim().isEmpty()) return;
            if (fileName.contains("/")) {
                fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
            }

            Path uploadsRoot = Paths.get(System.getProperty("user.dir")).resolve("uploads");
            Path signaturePath = uploadsRoot.resolve(fileName);
            if (!Files.exists(signaturePath)) return;

            Image img = Image.getInstance(signaturePath.toAbsolutePath().toString());
            img.scaleToFit(160, 80);
            img.setAlignment(Image.ALIGN_RIGHT);
            document.add(img);
        } catch (Exception e) {
            log.warn("Impossible d'ajouter le bloc signature attestation: {}", e.getMessage());
        }
    }

    private void addSignatureImageToCell(PdfPCell cell, CompanySettings settings) {
        try {
            if (settings == null) return;
            String fileName = settings.getSignatureFileName();
            if (fileName == null || fileName.trim().isEmpty()) return;
            if (fileName.contains("/")) {
                fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
            }
            Path uploadsRoot = Paths.get(System.getProperty("user.dir")).resolve("uploads");
            Path signaturePath = uploadsRoot.resolve(fileName);
            if (!Files.exists(signaturePath)) return;
            Image img = Image.getInstance(signaturePath.toAbsolutePath().toString());
            img.scaleToFit(140, 70);
            img.setAlignment(Image.ALIGN_LEFT);
            cell.addElement(img);
        } catch (Exception e) {
            log.warn("Impossible d'ajouter image signature au cell: {}", e.getMessage());
        }
    }

    private void addDividerLine(Document document) {
        try {
            PdfPTable t = new PdfPTable(1);
            t.setWidthPercentage(100);
            PdfPCell c = new PdfPCell(new Phrase(""));
            c.setBorder(Rectangle.BOTTOM);
            c.setBorderWidthBottom(0.7f);
            c.setPaddingTop(2);
            c.setPaddingBottom(2);
            c.setBorderColorBottom(new Color(200, 200, 200));
            t.addCell(c);
            document.add(t);
        } catch (Exception ignored) {
        }
    }

    private String orNA(String s) {
        return (s == null || s.trim().isEmpty()) ? "Non renseigné" : s;
    }

    private String defaultCity() {
        return "Tunis";
    }

    private String defaultEmployeeAddress() {
        return "tunis";
    }

    private String responsableFonction(ContractDTO contrat) {
        String role = (contrat != null) ? orNA(contrat.getRole()) : "Non renseigné";
        if ("Non renseigné".equals(role)) {
            return "responsable";
        }
        return "responsable " + role;
    }

    private String safe(String s) {
        return (s == null) ? "" : s;
    }

    private CompanySettings getOrDefaultSettings() {
        try {
            CompanySettings settings = settingsService.getSettings();
            if (settings != null) return settings;
        } catch (Exception e) {
            log.warn("Impossible de charger les paramètres société: " + e.getMessage());
        }
        
        // Valeurs par défaut
        CompanySettings defaults = new CompanySettings();
        defaults.setCompanyName("SMARTPARK SPORTS MANAGEMENT");
        defaults.setAddress("Rue de la Jeunesse, Tunis");
        defaults.setMatriculeFiscal("1785422/A/M/000");
        return defaults;
    }

    private void addLabelValue(PdfPTable table, String label, String value, Font bold, Font normal, Color border) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(5);
        cell.setBorderColor(border);
        Phrase ph = new Phrase();
        ph.add(new Chunk(label + " : ", bold));
        ph.add(new Chunk(value != null ? value : "", normal));
        cell.setPhrase(ph);
        table.addCell(cell);
    }

    private void addPayRow(PdfPTable table, String desc, String base, String taux, String nb, String gain, String retenue, Font font, Color bg, Color border) {
        PdfPCell cDesc = new PdfPCell(new Phrase(desc, font));
        cDesc.setBackgroundColor(bg);
        cDesc.setBorderColor(border);
        cDesc.setPadding(4);
        table.addCell(cDesc);

        String[] values = {base, taux, nb, gain, retenue};
        for (String v : values) {
            PdfPCell c = new PdfPCell(new Phrase(v != null ? v : "", font));
            c.setBackgroundColor(bg);
            c.setBorderColor(border);
            c.setPadding(4);
            c.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(c);
        }
    }

    private String fmt(double v) {
        if (v <= 0) return "";
        return String.format("%.3f", v).replace(".", ",");
    }

    private String fmtMoney(double v) {
        return String.format("%.3f", v).replace(".", ",");
    }

    private String fmtMoney(Double v) {
        if (v == null) return "0,000";
        return fmtMoney(v.doubleValue());
    }

    private String formatDate(java.time.LocalDate d) {
        return d != null ? d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "N/A";
    }

    @Override
    public String extractTextFromPdf(MultipartFile pdfFile) throws IOException {
        if (pdfFile == null || pdfFile.isEmpty()) {
            log.error("Fichier PDF vide ou null");
            throw new IllegalArgumentException("Le fichier PDF ne peut pas être vide");
        }

        return extractTextFromPdfBytes(pdfFile.getBytes());
    }

    @Override
    public String extractTextFromPdfBytes(byte[] pdfBytes) throws IOException {
        if (pdfBytes == null || pdfBytes.length == 0) {
            log.error("Contenu PDF vide ou null");
            throw new IllegalArgumentException("Le contenu PDF ne peut pas etre vide");
        }

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            log.info("✓ Texte extrait du PDF - {} caractères", text.length());
            return text;
        } catch (IOException e) {
            log.error("Erreur lors de l'extraction du texte du PDF", e);
            throw new IOException("Impossible d'extraire le texte du PDF", e);
        }
    }
}

