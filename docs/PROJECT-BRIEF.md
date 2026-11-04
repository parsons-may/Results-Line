# PROJECT-BRIEF — Results Line: a grounded healthcare phone assistant (working title)

Target role: Software Engineer I (healthcare / medtech)
Status: Pitch only. Nothing is built yet. Everything below is "will" unless listed under "Exists today."

## 1. What is it?

A simulated phone assistant in healthcare that will answer patients' questions about their procedure results, using only that patient's verified record, and hand everything else to staff. I will test it on GI questions, such as polyp results, to keep the scope narrow, but I am designing it so it could grow to other procedures and result types later.

When done, it will be a GitHub repository with the app (Spring Boot + React), synthetic patient records, caller test questions, and an evaluation log of wrong or falsely reassuring answers.

## 2. Why would anyone care?

A GI doctor told me his office mails result letters with the office number on them, and many patients with low-risk results call with questions. Staff spend time on these routine calls, which takes time away from patients with concerning results.

Alternatives I checked (Oct 2026):

- Current process: a mailed letter, then staff answer follow-up calls.
- Phone agents: PerfectServe says its agent will not read back test results; Novoflow's checks whether results are ready, does not read them, and points to the portal.
- Result messages: some vendors market notification agents, and Stanford Health Care built a tool that drafts plain-language result messages for physician approval.

My difference: answering a patient's live questions about one narrow result type, using only their verified record, with a published evaluation of how often it gets things dangerously wrong. My hypothesis is that this is hard to do safely, and the project tests that.

## 3. Who opens it, and what do they do?

- Reader: a hiring manager or engineer reviewing my portfolio.
- First 60 seconds: open the README, watch a short simulated call, and read the evaluation table, especially the false-reassurance rate and every failure.
- End user (imagined, not tested): a patient calling about a result letter.

## 4. What does it prove about me?

If it works as planned, a reviewer can fairly conclude that I can:

- Write prompt contracts: Claude must reply in a fixed structure, either an answer with the record fields it used, or a handoff to staff.
- Ground answers in a source.
- Build an agent loop with guardrails: identity check first, then answer or refuse.
- Evaluate an AI system.
- Ship tested engineering: doctor-set interval rules in plain code, unit tested, with Docker and CI.

Evidence will be the test suite, the evaluation log, and a write-up of the mistakes I caught. It will not prove the assistant is clinically safe, because it uses synthetic data only.

## 5. What is in, what is out, what comes first?

- In: synthetic records, identity check, Claude answering from the record, configurable interval rules, refusal and handoff, a simulated browser call (text first).
- Out (deliberately): real patient data, a real phone line, EHR integration, and medical advice beyond the patient's own result. The assistant never decides the follow-up interval.
- First thing to run: the rule engine on synthetic records. I'll know it ran when two tests pass: a low-risk record shows 10 years, and a higher-risk record, for example one with 5 or more polyps, shows a shorter follow-up. The doctor sets the real rules.

## 6. What could sink it?

Risk: the assistant tells a patient their result is fine when it isn't, or answers when it should have refused. Response: answers must cite record fields, anything unclear goes to staff, the test set includes tricky cases, and false reassurance is reported on its own line. If it can't be done safely, I will report that as the finding. Also, follow-up intervals are medical decisions I am not qualified to make, so the doctor sets the rules.

## 7. Why this project?

A mix of showing and learning.

- Already have: Java/Spring Boot and React plus Docker, Kubernetes, and Terraform. Evidence: my project Meow Matters (React), and my classes INFO 5100 (Java), INFO 6150 (Spring Boot) and CSYE 6225.
- New to me: prompt contracts for patient-facing answers, grounding verification, and building an evaluation harness.

I picked it because the problem came from a real doctor, and it's a full-stack app in healthcare, the field I want to work in.

## Exists today

- This brief
- Research notes on alternatives (Oct 2026), found with Claude and checked by me
- The doctor's description of the problem (verbal, anonymous)

## Will exist

- Synthetic patient records and labeled caller questions
- Rule engine with unit tests
- Identity check and Claude answer/refuse logic
- Simulated call screen
- Evaluation harness and results log
- Docker Compose setup and CI pipeline
- README with demo
