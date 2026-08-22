package com.isaqb.practice.portsandadapters.adapter.driving.file;

import com.isaqb.practice.portsandadapters.core.ApprovalDecision;
import com.isaqb.practice.portsandadapters.core.port.ApprovalRepository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A second ApprovalRepository implementation, backed by a flat file instead of an
 * in-process list. Deliberately a different storage technology from
 * InMemoryApprovalRepository, to make the interchangeability claim concrete rather
 * than a decorator around the same data structure.
 */
public class FileApprovalRepository implements ApprovalRepository {

    private static final String DELIMITER = "|";
    private static final String ESCAPED_DELIMITER = "\\|";

    private final Path file;

    public FileApprovalRepository(Path file) {
        this.file = file;
    }

    @Override
    public void save(ApprovalDecision decision) {
        // TODO: append one line to `file` (creating it if it doesn't exist yet -
        // Files.writeString(path, text, StandardOpenOption.CREATE,
        // StandardOpenOption.APPEND) does both at once), encoding `decision` as six
        // fields joined by DELIMITER, in this exact order: requester, approver,
        // namespace, approved (Boolean.toString), reason, decidedAt
        // (decision.decidedAt().toString()). End the line with "\n".
        // ApprovalRepository's contract declares no checked exceptions, so translate any
        // IOException into an UncheckedIOException - the same "adapter translates checked
        // I/O trouble into whatever its port promises" move FileConfigSource made for
        // ConfigLoadException in 01-layers, just landing on an unchecked exception here
        // because that's what *this* port promises.

        // just writing a dumb naieve implementation
        StringBuilder sb = new StringBuilder();
        sb.append(decision.requester()).append(DELIMITER);
        sb.append(decision.approver()).append(DELIMITER);
        sb.append(decision.namespace()).append(DELIMITER);
        sb.append(decision.approved()).append(DELIMITER);
        sb.append(decision.reason()).append(DELIMITER);
        sb.append(decision.decidedAt());
        sb.append("\n");
        var text = sb.toString();

        System.out.println("Attempting to save file to: " + file);
        System.out.println("Contents: " + text);
        if (!Files.exists(file)) {
            System.out.println("File " + file + " does not exist, creating it");
            try {
                Files.createFile(file);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        } else {
            System.out.println("File " + file + " already exist");
        }

        try {
            Files.writeString(file, text, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.out.println("Failed to save file to: " + file);
            throw new UncheckedIOException(e);
        }

    }

    @Override
    public List<ApprovalDecision> findByRequester(String requester) {
        // TODO:
        //  - if `file` does not exist yet, return an empty list (nobody has saved
        //    anything yet - a valid state, not an error).
        //  - otherwise read every line (Files.readAllLines), split each on DELIMITER
        //    (careful: "|" is a regex metacharacter - use Pattern.quote(DELIMITER), or
        //    the literal "\\|", as the split() argument), rebuild an ApprovalDecision per
        //    line (Boolean.parseBoolean, Instant.parse), and keep only the ones whose
        //    requester() equals `requester`, in file order.

        List<ApprovalDecision> decisions = new ArrayList<>();
        if (!Files.exists(file)) {
            System.out.println("File " + file + " does not exist, returning empty list");
            return  decisions;
        }

        List<String> lines = null;
        try {
            System.out.println("Attempting to read file from: " + file);
            lines = Files.readAllLines(file);
        } catch (IOException e) {
            System.out.println("Failed to read file from: " + file);
            throw new RuntimeException(e);
        }

        System.out.println("Read " + lines.size() + " lines");
        for (String line: lines){
            System.out.println("Parsing line: " + line);
            if (line.contains("|")) {
                String[] decisionSegments =  line.split(ESCAPED_DELIMITER);
                String requesterName = decisionSegments[0];
                if (!requesterName.trim().equalsIgnoreCase(requester)) {
                    continue;
                }

                String approverName = decisionSegments[1];
                String namespace = decisionSegments[2];
                String approvedText = decisionSegments[3];
                String reason = decisionSegments[4];
                String decidedAt = decisionSegments[5];

                boolean approved = Boolean.parseBoolean(approvedText);
                Instant decisionTime = Instant.parse(decidedAt);
                ApprovalDecision decision = new ApprovalDecision(
                        requesterName,
                        approverName,
                        namespace,
                        approved,
                        reason,
                        decisionTime
                );
                decisions.add(decision);
            } else {
                System.out.println("Line does not contain delimiter: " + line);
            }
        }

        return decisions;
    }
}
