package com.manuelpuchner.backend.mcc.service;

import com.manuelpuchner.backend.mcc.entity.MccCode;
import com.manuelpuchner.backend.mcc.repository.MccCodeRepository;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MccCodeImporter implements ApplicationRunner {

    private static final String CSV_PATH = "data/mcc_codes.csv";

    private final MccCodeRepository repository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if (repository.count() > 0) {
            log.debug("MCC codes already loaded, skipping import");
            return;
        }

        ClassPathResource resource = new ClassPathResource(CSV_PATH);
        if (!resource.exists()) {
            log.warn("MCC codes CSV not found at classpath:{} — skipping import", CSV_PATH);
            return;
        }

        List<MccCode> codes = parse(resource);
        repository.saveAll(codes);
        log.info("Imported {} MCC codes from {}", codes.size(), CSV_PATH);
    }

    private List<MccCode> parse(ClassPathResource resource) throws IOException, CsvException {
        try (var reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);
             var csv = new CSVReader(reader)) {

            List<String[]> rows = csv.readAll();
            List<MccCode> result = new ArrayList<>(rows.size() - 1);

            // skip header
            for (int i = 1; i < rows.size(); i++) {
                String[] r = rows.get(i);
                if (r.length < 6 || r[0].isBlank()) continue;

                result.add(MccCode.builder()
                        .mcc(r[0].trim())
                        .editedDescription(blankToNull(r[1]))
                        .combinedDescription(blankToNull(r[2]))
                        .usdaDescription(blankToNull(r[3]))
                        .irsDescription(blankToNull(r[4]))
                        .irsReportable(parseBoolean(r[5]))
                        .build());
            }
            return result;
        }
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private Boolean parseBoolean(String s) {
        if (s == null || s.isBlank()) return null;
        return "Yes".equalsIgnoreCase(s.trim());
    }
}
