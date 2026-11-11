# FRICTIONAL

Dated log of what broke, what changed in the plan, and what I did instead.

## 2026-11-04

We are now using Java 21 instead of Java 19.

## 2026-11-06

Currently responses are still vague and need to answer more questions rather than handing a good amount off.

This is about the demo mode keyword script, which hands off anything it does not recognize. It is not Claude. How often the real Claude hands off will be measured in the evaluation (step 5) against the labeled questions.

## 2026-11-11

We are not using the real Claude yet. The evaluation harness is built and tested, but it has only been run with the demo keyword script, not Claude, so there are no real evaluation results. I have not set up API access yet. Demo results are labeled and not committed.

I tested the beginning of the application with very basic questions, like: what is the risk, high risk versus low risk? When should I be seen? What are my results? Currently the responses are immediately being handed off. Some should be handed off, but some should be answered, so right now the responses are very vague. This is something to look into.
