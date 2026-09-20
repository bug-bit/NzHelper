import Foundation

struct Session: Identifiable, Codable, Equatable {
    var id: UUID = UUID()
    var timestamp: Date
    var duration: Int
    var remark: String
    var location: String
    var watchedMovie: Bool
    var climax: Bool
    var rating: Float
    var mood: String
    var props: String

    static var dateFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd'T'HH:mm:ss"
        f.locale = Locale(identifier: "en_US_POSIX")
        f.timeZone = TimeZone.current
        return f
    }()
}

extension JSONEncoder {
    static var sessionEncoder: JSONEncoder {
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .formatted(Session.dateFormatter)
        encoder.outputFormatting = .prettyPrinted
        return encoder
    }
}

extension JSONDecoder {
    static var sessionDecoder: JSONDecoder {
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .formatted(Session.dateFormatter)
        return decoder
    }
}
