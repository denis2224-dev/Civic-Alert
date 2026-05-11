package com.civicalert.config;

import com.civicalert.entity.OfficialInfo;
import com.civicalert.entity.RumorPattern;
import com.civicalert.entity.VerifiedClaim;
import com.civicalert.enums.ClaimStatus;
import com.civicalert.repository.OfficialInfoRepository;
import com.civicalert.repository.RumorPatternRepository;
import com.civicalert.repository.VerifiedClaimRepository;
import com.civicalert.util.TextNormalizer;
import java.util.List;
import java.util.Locale;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final OfficialInfoRepository officialInfoRepository;
    private final RumorPatternRepository rumorPatternRepository;
    private final VerifiedClaimRepository verifiedClaimRepository;

    public DataSeeder(
            OfficialInfoRepository officialInfoRepository,
            RumorPatternRepository rumorPatternRepository,
            VerifiedClaimRepository verifiedClaimRepository
    ) {
        this.officialInfoRepository = officialInfoRepository;
        this.rumorPatternRepository = rumorPatternRepository;
        this.verifiedClaimRepository = verifiedClaimRepository;
    }

    @Override
    public void run(String... args) {
        seedOfficialInfo();
        seedRumorPatterns();
        seedVerifiedClaims();
    }

    private void seedOfficialInfo() {
        List<OfficialSeed> officialSeeds = List.of(
                new OfficialSeed("voting_hours", "Polling stations are open from 07:00 to 21:00 on election day.", "en"),
                new OfficialSeed("election_date", "Election day is announced only through official election communications.", "en"),
                new OfficialSeed("sms_voting", "Voting by SMS is not available. Any such claim is misleading.", "en"),
                new OfficialSeed("online_voting", "Online voting is not available for this election.", "en"),
                new OfficialSeed("mobile_voting", "Voting through mobile applications is not part of the official procedure.", "en"),
                new OfficialSeed("early_voting", "Early voting follows official legal schedules and locations only.", "en"),
                new OfficialSeed("polling_station_lookup", "Voters should verify their polling station only through official sources.", "en"),
                new OfficialSeed("polling_station_change", "Any polling station change is announced only by official election authorities.", "en"),
                new OfficialSeed("required_documents", "Voters must present a legally accepted identification document.", "en"),
                new OfficialSeed("expired_documents", "Expired documents may not be accepted. Check official guidance before voting.", "en"),
                new OfficialSeed("student_voting", "Students vote according to legal residence or temporary voting rules published officially.", "en"),
                new OfficialSeed("diaspora_voting", "Diaspora voting is available only in officially announced locations.", "en"),
                new OfficialSeed("ballot_marking", "Ballots must be marked according to official instructions in the polling station.", "en"),
                new OfficialSeed("ballot_photos", "Taking photos of completed ballots may violate election rules.", "en"),
                new OfficialSeed("ballot_invalid_rules", "A ballot can become invalid if marked incorrectly. Follow official instructions.", "en"),
                new OfficialSeed("assistance_for_disabled_voters", "Voters with disabilities can request assistance according to official procedures.", "en"),
                new OfficialSeed("observer_rules", "Observers follow accredited legal rules and cannot alter voting results.", "en"),
                new OfficialSeed("election_cancelled", "The election has not been cancelled. Any cancellation would be announced officially.", "en"),
                new OfficialSeed("election_postponed", "The election has not been postponed unless officially announced.", "en"),
                new OfficialSeed("results_publication", "Official election results are published only by the authorized election body.", "en"),
                new OfficialSeed("preliminary_results", "Preliminary results are unofficial and may change after verification.", "en"),
                new OfficialSeed("final_results", "Final results are published after legal counting and validation procedures.", "en"),
                new OfficialSeed("complaints_process", "Election complaints must be submitted through the official legal process.", "en"),
                new OfficialSeed("emergency_contacts", "Emergency election support contacts are published only on official channels.", "en"),
                new OfficialSeed("official_sources", "Use only official websites and hotlines for election-process information.", "en"),

                new OfficialSeed("voting_hours_ro", "Secțiile de votare sunt deschise între 07:00 și 21:00 în ziua alegerilor.", "ro"),
                new OfficialSeed("sms_voting_ro", "Votarea prin SMS nu este disponibilă.", "ro"),
                new OfficialSeed("online_voting_ro", "Votarea online nu este disponibilă pentru aceste alegeri.", "ro"),
                new OfficialSeed("polling_station_lookup_ro", "Secția de votare se verifică doar din surse oficiale.", "ro"),
                new OfficialSeed("election_cancelled_ro", "Alegerile nu au fost anulate.", "ro"),
                new OfficialSeed("results_publication_ro", "Rezultatele oficiale sunt publicate doar de autoritatea electorală.", "ro"),

                new OfficialSeed("voting_hours_ru", "Избирательные участки открыты с 07:00 до 21:00 в день выборов.", "ru"),
                new OfficialSeed("sms_voting_ru", "Голосование по СМС недоступно.", "ru"),
                new OfficialSeed("online_voting_ru", "Онлайн-голосование недоступно для этих выборов.", "ru"),
                new OfficialSeed("polling_station_lookup_ru", "Проверяйте свой участок только через официальные источники.", "ru"),
                new OfficialSeed("election_cancelled_ru", "Выборы не отменены.", "ru"),
                new OfficialSeed("results_publication_ru", "Официальные результаты публикуются только уполномоченным органом.", "ru")
        );

        for (OfficialSeed officialSeed : officialSeeds) {
            seedOfficial(officialSeed.topic(), officialSeed.content(), officialSeed.language());
        }
    }

    private void seedOfficial(String topic, String content, String language) {
        if (officialInfoRepository.findByTopic(topic).isPresent()) {
            return;
        }

        OfficialInfo info = new OfficialInfo();
        info.setTopic(topic);
        info.setContent(content);
        info.setSourceName("Central Electoral Commission");
        info.setSourceUrl("https://cec.example/demo");
        info.setLanguage(language);
        officialInfoRepository.save(info);
    }

    private void seedRumorPatterns() {
        List<PatternSeed> patterns = List.of(
                new PatternSeed("voting cancelled", "voting_process", 5, "en"),
                new PatternSeed("election cancelled", "voting_process", 5, "en"),
                new PatternSeed("election postponed", "voting_date", 5, "en"),
                new PatternSeed("voting moved to tomorrow", "voting_date", 5, "en"),
                new PatternSeed("voting date changed", "voting_date", 5, "en"),
                new PatternSeed("polls open tomorrow instead", "voting_date", 4, "en"),
                new PatternSeed("polling starts at noon", "voting_hours", 4, "en"),
                new PatternSeed("polling stations open only until 18 00", "voting_hours", 4, "en"),
                new PatternSeed("do not vote today", "voter_suppression", 5, "en"),
                new PatternSeed("skip voting to avoid fines", "voter_suppression", 4, "en"),
                new PatternSeed("polling stations are closed", "polling_location", 5, "en"),
                new PatternSeed("polling station moved", "polling_location", 4, "en"),
                new PatternSeed("your polling station changed", "polling_location", 4, "en"),
                new PatternSeed("all polling stations moved downtown", "polling_location", 4, "en"),
                new PatternSeed("vote by sms", "fake_voting_method", 5, "en"),
                new PatternSeed("vote by text message", "fake_voting_method", 5, "en"),
                new PatternSeed("vote online", "fake_voting_method", 5, "en"),
                new PatternSeed("vote by phone", "fake_voting_method", 5, "en"),
                new PatternSeed("mobile app voting is active", "fake_voting_method", 5, "en"),
                new PatternSeed("ballots with pen are invalid", "ballot_confusion", 4, "en"),
                new PatternSeed("ballots marked with stamp are invalid", "ballot_confusion", 4, "en"),
                new PatternSeed("ballot with two marks is still valid", "ballot_confusion", 3, "en"),
                new PatternSeed("bring only passport", "id_requirement", 3, "en"),
                new PatternSeed("id cards are not accepted", "id_requirement", 4, "en"),
                new PatternSeed("results already decided", "results_misinformation", 4, "en"),
                new PatternSeed("official results leaked", "results_misinformation", 4, "en"),
                new PatternSeed("observers are banned", "observer_misinformation", 3, "en"),
                new PatternSeed("observers can remove voters from line", "observer_misinformation", 3, "en"),
                new PatternSeed("diaspora votes cancelled", "diaspora_voting", 5, "en"),
                new PatternSeed("fake election hotline confirms cancellation", "official_impersonation", 4, "en"),
                new PatternSeed("commission website says vote by sms", "official_impersonation", 5, "en"),

                new PatternSeed("alegerile au fost anulate", "voting_process", 5, "ro"),
                new PatternSeed("votarea a fost anulată", "voting_process", 5, "ro"),
                new PatternSeed("alegerile au fost amânate", "voting_date", 5, "ro"),
                new PatternSeed("votarea s-a mutat pe mâine", "voting_date", 5, "ro"),
                new PatternSeed("data votării s-a schimbat", "voting_date", 5, "ro"),
                new PatternSeed("secțiile se deschid la ora 12", "voting_hours", 4, "ro"),
                new PatternSeed("secțiile se închid la 18 00", "voting_hours", 4, "ro"),
                new PatternSeed("nu mergeți la vot astăzi", "voter_suppression", 5, "ro"),
                new PatternSeed("nu votați azi nu contează", "voter_suppression", 4, "ro"),
                new PatternSeed("secțiile de votare sunt închise", "polling_location", 5, "ro"),
                new PatternSeed("secția de votare s-a mutat", "polling_location", 4, "ro"),
                new PatternSeed("toate secțiile s-au mutat în centru", "polling_location", 4, "ro"),
                new PatternSeed("poți vota prin sms", "fake_voting_method", 5, "ro"),
                new PatternSeed("vot prin sms", "fake_voting_method", 5, "ro"),
                new PatternSeed("poți vota online", "fake_voting_method", 5, "ro"),
                new PatternSeed("vot online", "fake_voting_method", 5, "ro"),
                new PatternSeed("vot prin telefon", "fake_voting_method", 5, "ro"),
                new PatternSeed("buletinele completate cu pixul sunt invalide", "ballot_confusion", 4, "ro"),
                new PatternSeed("ștampila face buletinul invalid", "ballot_confusion", 4, "ro"),
                new PatternSeed("cartea de identitate nu este acceptată", "id_requirement", 4, "ro"),
                new PatternSeed("se acceptă doar pașaportul", "id_requirement", 3, "ro"),
                new PatternSeed("rezultatele sunt deja decise", "results_misinformation", 4, "ro"),
                new PatternSeed("rezultatele oficiale au fost scurse", "results_misinformation", 4, "ro"),
                new PatternSeed("observatorii sunt interziși", "observer_misinformation", 3, "ro"),
                new PatternSeed("observatorii pot opri votanții", "observer_misinformation", 3, "ro"),
                new PatternSeed("voturile diasporei au fost anulate", "diaspora_voting", 5, "ro"),
                new PatternSeed("site-ul oficial confirmă vot prin sms", "official_impersonation", 5, "ro"),
                new PatternSeed("linia oficială spune că alegerile sunt anulate", "official_impersonation", 4, "ro"),

                new PatternSeed("выборы отменены", "voting_process", 5, "ru"),
                new PatternSeed("голосование отменено", "voting_process", 5, "ru"),
                new PatternSeed("выборы перенесли", "voting_date", 5, "ru"),
                new PatternSeed("голосование перенесли на завтра", "voting_date", 5, "ru"),
                new PatternSeed("дата голосования изменена", "voting_date", 5, "ru"),
                new PatternSeed("участки открываются в 12 00", "voting_hours", 4, "ru"),
                new PatternSeed("участки закрываются в 18 00", "voting_hours", 4, "ru"),
                new PatternSeed("не ходите голосовать сегодня", "voter_suppression", 5, "ru"),
                new PatternSeed("сегодня голосовать не нужно", "voter_suppression", 4, "ru"),
                new PatternSeed("избирательные участки закрыты", "polling_location", 5, "ru"),
                new PatternSeed("ваш участок изменился", "polling_location", 4, "ru"),
                new PatternSeed("все участки перенесли в центр", "polling_location", 4, "ru"),
                new PatternSeed("можно голосовать по смс", "fake_voting_method", 5, "ru"),
                new PatternSeed("голосование по смс", "fake_voting_method", 5, "ru"),
                new PatternSeed("можно голосовать онлайн", "fake_voting_method", 5, "ru"),
                new PatternSeed("онлайн голосование доступно", "fake_voting_method", 5, "ru"),
                new PatternSeed("голосование по телефону", "fake_voting_method", 5, "ru"),
                new PatternSeed("бюллетени недействительны", "ballot_confusion", 4, "ru"),
                new PatternSeed("бюллетень с ручкой недействителен", "ballot_confusion", 4, "ru"),
                new PatternSeed("удостоверения личности не принимаются", "id_requirement", 4, "ru"),
                new PatternSeed("принимают только паспорт", "id_requirement", 3, "ru"),
                new PatternSeed("результаты уже решены", "results_misinformation", 4, "ru"),
                new PatternSeed("официальные результаты утекли", "results_misinformation", 4, "ru"),
                new PatternSeed("наблюдатели запрещены", "observer_misinformation", 3, "ru"),
                new PatternSeed("наблюдатели могут не пустить на голосование", "observer_misinformation", 3, "ru"),
                new PatternSeed("голоса диаспоры отменены", "diaspora_voting", 5, "ru"),
                new PatternSeed("официальный сайт разрешил смс голосование", "official_impersonation", 5, "ru"),
                new PatternSeed("горячая линия цик подтвердила отмену выборов", "official_impersonation", 4, "ru")
        );

        for (PatternSeed pattern : patterns) {
            String normalized = TextNormalizer.normalize(pattern.phrase());
            if (rumorPatternRepository.findByNormalizedPhraseAndLanguage(normalized, pattern.language()).isPresent()) {
                continue;
            }

            RumorPattern entity = new RumorPattern();
            entity.setPhrase(pattern.phrase());
            entity.setNormalizedPhrase(normalized);
            entity.setCategory(pattern.category());
            entity.setSeverity(pattern.severity());
            entity.setLanguage(pattern.language());
            entity.setActive(true);
            rumorPatternRepository.save(entity);
        }
    }

    private void seedVerifiedClaims() {
        List<VerifiedClaimSeed> claims = List.of(
                new VerifiedClaimSeed(
                        "You can vote by SMS.",
                        "fake_voting_method",
                        ClaimStatus.VERIFIED_FALSE,
                        "Voting by SMS is not available. Use only official voting procedures.",
                        "en",
                        "Moldova"
                ),
                new VerifiedClaimSeed(
                        "The election was cancelled.",
                        "voting_process",
                        ClaimStatus.VERIFIED_FALSE,
                        "The election has not been cancelled. Polling stations operate according to the official schedule.",
                        "en",
                        "Moldova"
                ),
                new VerifiedClaimSeed(
                        "Voting is online.",
                        "fake_voting_method",
                        ClaimStatus.VERIFIED_FALSE,
                        "Online voting is not available for this election.",
                        "en",
                        "Moldova"
                ),
                new VerifiedClaimSeed(
                        "Polling stations are open from 07:00 to 21:00.",
                        "voting_hours",
                        ClaimStatus.VERIFIED_TRUE,
                        "Correct. Polling stations are open from 07:00 to 21:00.",
                        "en",
                        "Moldova"
                ),
                new VerifiedClaimSeed(
                        "Voting is from 07:00 to 21:00.",
                        "voting_hours",
                        ClaimStatus.VERIFIED_TRUE,
                        "Correct. Polling stations are open from 07:00 to 21:00.",
                        "en",
                        "Moldova"
                ),
                new VerifiedClaimSeed(
                        "Results are already official before polls close.",
                        "results_misinformation",
                        ClaimStatus.VERIFIED_FALSE,
                        "Official results are published only after legal counting and reporting procedures.",
                        "en",
                        "Moldova"
                ),
                new VerifiedClaimSeed(
                        "You need only a passport to vote.",
                        "id_requirement",
                        ClaimStatus.NEEDS_CONTEXT,
                        "Accepted documents depend on official election regulations. Check the official requirements list.",
                        "en",
                        "Moldova"
                ),
                new VerifiedClaimSeed(
                        "Identity cards are not accepted.",
                        "id_requirement",
                        ClaimStatus.VERIFIED_FALSE,
                        "Identity cards are accepted according to official election rules.",
                        "en",
                        "Moldova"
                ),
                new VerifiedClaimSeed(
                        "Polling station information is available only on official sources.",
                        "polling_location",
                        ClaimStatus.VERIFIED_TRUE,
                        "Correct. Verify polling locations only via official election sources.",
                        "en",
                        "Moldova"
                ),
                new VerifiedClaimSeed(
                        "All ballots marked with pen are invalid.",
                        "ballot_confusion",
                        ClaimStatus.MISLEADING,
                        "Ballot validity depends on official marking rules, not a blanket pen-only rule.",
                        "en",
                        "Moldova"
                ),
                new VerifiedClaimSeed(
                        "Diaspora voting was cancelled.",
                        "diaspora_voting",
                        ClaimStatus.VERIFIED_FALSE,
                        "Diaspora voting has not been cancelled. Use official lists of polling stations abroad.",
                        "en",
                        "Diaspora"
                ),
                new VerifiedClaimSeed(
                        "Observers are banned from polling stations.",
                        "observer_misinformation",
                        ClaimStatus.VERIFIED_FALSE,
                        "Authorized observers are allowed under official election law.",
                        "en",
                        "Moldova"
                ),

                new VerifiedClaimSeed(
                        "Poți vota prin SMS.",
                        "fake_voting_method",
                        ClaimStatus.VERIFIED_FALSE,
                        "Votarea prin SMS nu este disponibilă.",
                        "ro",
                        "Moldova"
                ),
                new VerifiedClaimSeed(
                        "Alegerile au fost anulate.",
                        "voting_process",
                        ClaimStatus.VERIFIED_FALSE,
                        "Alegerile nu au fost anulate.",
                        "ro",
                        "Moldova"
                ),
                new VerifiedClaimSeed(
                        "Poți vota online.",
                        "fake_voting_method",
                        ClaimStatus.VERIFIED_FALSE,
                        "Votarea online nu este disponibilă.",
                        "ro",
                        "Moldova"
                ),
                new VerifiedClaimSeed(
                        "Secțiile sunt deschise între 07:00 și 21:00.",
                        "voting_hours",
                        ClaimStatus.VERIFIED_TRUE,
                        "Corect. Programul oficial este 07:00 - 21:00.",
                        "ro",
                        "Moldova"
                ),
                new VerifiedClaimSeed(
                        "Cartea de identitate nu este acceptată la vot.",
                        "id_requirement",
                        ClaimStatus.VERIFIED_FALSE,
                        "Cartea de identitate este acceptată conform regulilor oficiale.",
                        "ro",
                        "Moldova"
                ),
                new VerifiedClaimSeed(
                        "Rezultatele sunt oficiale înainte de închiderea secțiilor.",
                        "results_misinformation",
                        ClaimStatus.VERIFIED_FALSE,
                        "Rezultatele oficiale apar după închiderea secțiilor și validarea legală.",
                        "ro",
                        "Moldova"
                ),
                new VerifiedClaimSeed(
                        "Voturile diasporei au fost anulate.",
                        "diaspora_voting",
                        ClaimStatus.VERIFIED_FALSE,
                        "Voturile diasporei nu au fost anulate.",
                        "ro",
                        "Diaspora"
                ),

                new VerifiedClaimSeed(
                        "Можно голосовать по СМС.",
                        "fake_voting_method",
                        ClaimStatus.VERIFIED_FALSE,
                        "Голосование по СМС недоступно.",
                        "ru",
                        "Moldova"
                ),
                new VerifiedClaimSeed(
                        "Выборы отменены.",
                        "voting_process",
                        ClaimStatus.VERIFIED_FALSE,
                        "Выборы не отменены.",
                        "ru",
                        "Moldova"
                ),
                new VerifiedClaimSeed(
                        "Можно голосовать онлайн.",
                        "fake_voting_method",
                        ClaimStatus.VERIFIED_FALSE,
                        "Онлайн-голосование недоступно.",
                        "ru",
                        "Moldova"
                ),
                new VerifiedClaimSeed(
                        "Участки открыты с 07:00 до 21:00.",
                        "voting_hours",
                        ClaimStatus.VERIFIED_TRUE,
                        ClaimCheckTexts.RU_VOTING_HOURS,
                        "ru",
                        "Moldova"
                ),
                new VerifiedClaimSeed(
                        "Удостоверение личности не принимается на участке.",
                        "id_requirement",
                        ClaimStatus.VERIFIED_FALSE,
                        "Удостоверение личности принимается по официальным правилам.",
                        "ru",
                        "Moldova"
                ),
                new VerifiedClaimSeed(
                        "Официальные результаты объявлены до закрытия участков.",
                        "results_misinformation",
                        ClaimStatus.VERIFIED_FALSE,
                        "Официальные результаты публикуются после закрытия участков и законной проверки.",
                        "ru",
                        "Moldova"
                ),
                new VerifiedClaimSeed(
                        "Голоса диаспоры отменены.",
                        "diaspora_voting",
                        ClaimStatus.VERIFIED_FALSE,
                        "Голоса диаспоры не отменены.",
                        "ru",
                        "Diaspora"
                )
        );

        for (VerifiedClaimSeed claim : claims) {
            seedVerifiedClaim(
                    claim.claimText(),
                    claim.category(),
                    claim.status(),
                    claim.correctionText(),
                    claim.language(),
                    claim.region()
            );
        }
    }

    private void seedVerifiedClaim(
            String claimText,
            String category,
            ClaimStatus status,
            String correctionText,
            String language,
            String region
    ) {
        String normalized = TextNormalizer.normalize(claimText);
        String normalizedLanguage = language.trim().toLowerCase(Locale.ROOT);
        if (verifiedClaimRepository.findFirstByNormalizedClaimAndLanguageAndPublishedTrue(normalized, normalizedLanguage).isPresent()) {
            return;
        }

        VerifiedClaim claim = new VerifiedClaim();
        claim.setClaimText(claimText);
        claim.setNormalizedClaim(normalized);
        claim.setCategory(category);
        claim.setStatus(status);
        claim.setCorrectionText(correctionText);
        claim.setOfficialSource("Central Electoral Commission");
        claim.setOfficialSourceUrl("https://cec.example/demo");
        claim.setLanguage(normalizedLanguage);
        claim.setRegion(region);
        claim.setPublished(true);
        verifiedClaimRepository.save(claim);
    }

    private record OfficialSeed(String topic, String content, String language) {
    }

    private record PatternSeed(String phrase, String category, int severity, String language) {
    }

    private record VerifiedClaimSeed(
            String claimText,
            String category,
            ClaimStatus status,
            String correctionText,
            String language,
            String region
    ) {
    }

    private static final class ClaimCheckTexts {
        private static final String RU_VOTING_HOURS = "Верно. Участки открыты с 07:00 до 21:00.";

        private ClaimCheckTexts() {
        }
    }
}
