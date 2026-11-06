package com.resultsline.assistant;

import com.resultsline.data.PatientRecord;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks an answer in plain code before it reaches the caller (hard rules 3 and 4).
 *
 * <p>Checks: cites at least one known field; no duplicate fields; every number in the answer text
 * appears in a cited field. Known limit: numbers written as words ("five") are not checked, and
 * non-numeric claims are not verified. The evaluation harness (step 5) measures what slips through.
 */
public final class ReplyValidator {

    private static final Pattern NUMBER = Pattern.compile("\\d+");
    private static final int MAX_ANSWER_CHARS = 600;

    private ReplyValidator() {
    }

    /** @return problems found; empty means the answer passed */
    public static List<String> validate(AssistantReply.Answer answer, Map<String, Object> facts) {
        List<String> problems = new ArrayList<>();

        if (answer.fieldsUsed().isEmpty()) {
            problems.add("answer cites no record fields");
        }
        Set<String> seen = new HashSet<>();
        for (String field : answer.fieldsUsed()) {
            if (!PatientRecord.CITABLE_FIELDS.contains(field)) {
                problems.add("cites unknown field '" + field + "'");
            } else if (!seen.add(field)) {
                problems.add("cites field '" + field + "' twice");
            }
        }
        if (answer.text().length() > MAX_ANSWER_CHARS) {
            problems.add("answer is longer than " + MAX_ANSWER_CHARS + " characters");
        }

        Set<Integer> allowedNumbers = new HashSet<>();
        for (String field : answer.fieldsUsed()) {
            if (facts.containsKey(field)) {
                collectNumbers(String.valueOf(facts.get(field)), allowedNumbers);
            }
        }
        Set<Integer> answerNumbers = new HashSet<>();
        collectNumbers(answer.text(), answerNumbers);
        for (Integer n : answerNumbers) {
            if (!allowedNumbers.contains(n)) {
                problems.add("number " + n + " in the answer is not in any cited field");
            }
        }
        return problems;
    }

    private static void collectNumbers(String text, Set<Integer> into) {
        Matcher m = NUMBER.matcher(text);
        while (m.find()) {
            try {
                into.add(Integer.parseInt(m.group()));
            } catch (NumberFormatException e) {
                into.add(Integer.MAX_VALUE); // absurdly long digit string: cannot match a record value
            }
        }
    }
}
