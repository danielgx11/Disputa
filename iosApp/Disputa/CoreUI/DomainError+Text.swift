import Shared

extension DomainError {
    var localizedMessage: String {
        switch self {
        case .teamnamerequired: String(localized: "error_team_name")
        case .teaminuse: String(localized: "error_team_in_use")
        case .championshipnamerequired: String(localized: "error_championship_name")
        case .knockoutteamcountnotpoweroftwo: String(localized: "error_ko_power")
        case .unevengroups: String(localized: "error_uneven_groups")
        case .groupstoosmall: String(localized: "error_groups_small")
        case .needtwogroups: String(localized: "error_need_groups")
        case .qualifiercountinvalid: String(localized: "error_qualifiers")
        case .thirdplacenotallowed: String(localized: "error_third_place")
        case .matchnotfound: String(localized: "error_match")
        case .championshipclosed: String(localized: "error_closed")
        case .priorphaselocked: String(localized: "error_locked")
        case .knockoutdrawneedspenaltywinner: String(localized: "error_penalties")
        case .invalidscore: String(localized: "error_score")
        case .unresolvedtie: String(localized: "error_tie")
        case .championundefined: String(localized: "error_champion")
        case .thirdplaceincomplete: String(localized: "error_third_incomplete")
        case .teamnotinregistry: String(localized: "error_team_missing")
        case .notenoughteams: String(localized: "error_not_enough")
        case .bracketnotpoweroftwo: String(localized: "error_bracket")
        case .fillqualifiersrequired: String(localized: "error_fill")
        case .knockoutalreadystarted: String(localized: "error_ko_started")
        case .fixturesalreadyplayed: String(localized: "error_fixtures_played")
        case .sameteamtwice: String(localized: "error_same_team")
        default: String(localized: "error_generic")
        }
    }
}
