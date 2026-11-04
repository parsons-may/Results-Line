# CLAUDE.md: Results Line

A simulated phone assistant in healthcare that answers patients' questions about their procedure results using only that patient's verified record, and hands everything else to staff. Full plan: [docs/PROJECT-BRIEF.md](docs/PROJECT-BRIEF.md). Read it before building anything.

This is a portfolio project for a Software Engineer I role (healthcare / medtech). It is also a course final project, so the repo must stay honest about what exists and what is only planned.

## Hard rules

1. **Synthetic data only.** No real patient data, ever. All records and caller questions are made up. Never commit real names, phone numbers, or anything that looks like a real record.
2. **The assistant never decides a follow-up interval.** Interval rules are plain, configurable code (not AI), set by the doctor, and fully unit tested. Until the doctor confirms real thresholds, use clearly labeled placeholder values. Do not present placeholders as medical guidance.
3. **Grounded answers only.** Every answer must come from the verified patient's record and cite the record fields it used. If it can't be grounded, or the question is unclear, it hands off to staff.
4. **Fixed reply structure (the prompt contract).** Claude replies in one of two shapes: an answer plus the record fields used, or a handoff to staff. Validate the structure in code; don't trust the model to follow it.
5. **Identity check first.** No record information is given before the caller is verified.
6. **No medical advice** beyond the patient's own result. Refuse and hand off.
7. **False reassurance is the key failure.** Telling a patient they're fine when they're not must be counted and reported on its own line in the evaluation, never averaged away.
8. **No secrets in git.** API keys go in environment variables, never in the repo. Use a `.env.example` with placeholder names.
9. **Honest docs.** README says "built" only for things that run. Anything planned says "planned". Log changes of plan and things that broke in `FRICTIONAL.md` with dates.

## Stack

Spring Boot (Java) backend, React frontend, Claude API for answers, Docker Compose for local runs, GitHub Actions CI running the tests on every commit. Text call simulation in the browser first; voice only if time allows.

## Scope

In: synthetic records, identity check, Claude answering from the record, configurable interval rules, refusal and handoff, simulated call, evaluation harness with labeled caller questions (including ones it must refuse), results log.

Out on purpose: real patient data, a real phone line, EHR integration, any medical advice beyond the patient's own result.

Start narrow: GI results such as polyp results. Design so other procedures and result types could be added later, but don't build them now.

## Build order

1. **Rule engine on synthetic records, with unit tests.** Done when two tests pass: a low-risk record returns 10 years, and a higher-risk record (for example 5 or more polyps) returns a shorter follow-up. The doctor sets the real rules, so keep thresholds in config.
2. Synthetic patient records and labeled caller questions.
3. Identity check, then Claude answer/refuse logic with the fixed reply structure and a validator.
4. Simulated call screen (React, text first).
5. Evaluation harness: score correct answers, correct refusals, and false reassurance separately. Log every failure.
6. Docker Compose and CI.
7. README with a short demo and the evaluation table.

Work one step at a time. Run the tests before saying something works. Show failures honestly.
