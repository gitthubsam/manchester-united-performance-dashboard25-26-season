import java.time.LocalDate;
import java.util.Objects;

/** An actual Manchester United result in the 2025/26 Premier League season. */
final class Match {
    final LocalDate date;
    final String season, competition, opponent, venue;
    final int goalsFor, goalsAgainst;

    Match(LocalDate date, String season, String competition, String opponent,
          String venue, int goalsFor, int goalsAgainst) {
        this.date = Objects.requireNonNull(date);
        this.season = Objects.requireNonNull(season);
        this.competition = Objects.requireNonNull(competition);
        this.opponent = Objects.requireNonNull(opponent);
        this.venue = Objects.requireNonNull(venue);
        if (goalsFor < 0 || goalsAgainst < 0) throw new IllegalArgumentException("Goals cannot be negative");
        this.goalsFor = goalsFor;
        this.goalsAgainst = goalsAgainst;
    }

    char result() {
        return goalsFor > goalsAgainst ? 'W' : goalsFor == goalsAgainst ? 'D' : 'L';
    }
}
