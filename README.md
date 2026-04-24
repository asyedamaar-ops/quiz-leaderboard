# Quiz Leaderboard - SRM Internship Task

Built this for the Bajaj Finserv Health Java qualifier assignment.

## What it does
Calls the quiz API 10 times, collects scores, removes duplicate entries and submits the final leaderboard.

## Problem I ran into
The API sends the same round data multiple times across polls. If you add scores every time you see them, the total becomes wrong. Fixed this by tracking which roundId + participant combos were already counted.

## How to run

Make sure you have Java 11+ and Maven installed.

Clone the repo:

    git clone https://github.com/asyedamaar-ops/quiz-leaderboard.git
    cd quiz-leaderboard

Put your reg number in QuizLeaderboard.java line 7.

Build:

    mvn clean package -q

Run:

    java -jar target/quiz-leaderboard.jar

Takes around 50 seconds because of the 5 second delay between each poll.
