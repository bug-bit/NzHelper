package me.neko.nzhelper.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BackHand
import androidx.compose.material.icons.outlined.Bathroom
import androidx.compose.material.icons.outlined.Bathtub
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.BeachAccess
import androidx.compose.material.icons.outlined.Bed
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.BedtimeOff
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.Chair
import androidx.compose.material.icons.outlined.CleanHands
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.DirectionsBike
import androidx.compose.material.icons.outlined.DirectionsBoat
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.ElectricCar
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.EventSeat
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Forest
import androidx.compose.material.icons.outlined.Gamepad
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Handyman
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Healing
import androidx.compose.material.icons.outlined.HeartBroken
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.HotTub
import androidx.compose.material.icons.outlined.Icecream
import androidx.compose.material.icons.outlined.Iron
import androidx.compose.material.icons.outlined.KingBed
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.LocalBar
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.LocalLaundryService
import androidx.compose.material.icons.outlined.LocalMall
import androidx.compose.material.icons.outlined.LocalTaxi
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.Nightlight
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Piano
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Pool
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.Sailing
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.SentimentDissatisfied
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material.icons.outlined.SentimentVerySatisfied
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Sick
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.Soap
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.SportsBaseball
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.SportsGolf
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material.icons.outlined.SportsTennis
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.Toys
import androidx.compose.material.icons.outlined.Train
import androidx.compose.material.icons.outlined.Tram
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.Wc
import androidx.compose.material.icons.outlined.Weekend
import androidx.compose.material.icons.outlined.WineBar
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material.icons.outlined.Yard
import androidx.compose.ui.graphics.vector.ImageVector

object TagIcons {

    private val map = mapOf(
        "tag" to Icons.Outlined.Tag,
        "folder" to Icons.Outlined.Folder,
        "hash" to Icons.Outlined.Sell,
        "hand" to Icons.Outlined.BackHand,
        "category" to Icons.Outlined.Category,
        "mood" to Icons.Outlined.Mood,
        "place" to Icons.Outlined.Place,
        "map-pin" to Icons.Outlined.Place,
        "bed" to Icons.Outlined.Bed,
        "bed-double" to Icons.Outlined.Bed,
        "king-bed" to Icons.Outlined.KingBed,
        "sofa" to Icons.Outlined.Weekend,
        "event-seat" to Icons.Outlined.EventSeat,
        "chair" to Icons.Outlined.Chair,
        "shower-head" to Icons.Outlined.Bathroom,
        "bathtub" to Icons.Outlined.Bathtub,
        "hot-tub" to Icons.Outlined.HotTub,
        "door-closed" to Icons.Outlined.MeetingRoom,
        "briefcase" to Icons.Outlined.Work,
        "building-2" to Icons.Outlined.Apartment,
        "home" to Icons.Outlined.Home,
        "landscape" to Icons.Outlined.Landscape,
        "park" to Icons.Outlined.Park,
        "forest" to Icons.Outlined.Forest,
        "yard" to Icons.Outlined.Yard,
        "nature" to Icons.Outlined.LocalFlorist,
        "beach" to Icons.Outlined.BeachAccess,
        "pool" to Icons.Outlined.Pool,
        "waves" to Icons.Outlined.Waves,
        "wind" to Icons.Outlined.Air,
        "snowflake" to Icons.Outlined.AcUnit,
        "clock" to Icons.Outlined.AccessTime,
        "sunrise" to Icons.Outlined.LightMode,
        "sun" to Icons.Outlined.WbSunny,
        "sunset" to Icons.Outlined.NightsStay,
        "moon" to Icons.Outlined.Bedtime,
        "moon-star" to Icons.Outlined.BedtimeOff,
        "nightlight" to Icons.Outlined.Nightlight,
        "calendar" to Icons.Outlined.CalendarToday,
        "calendar-days" to Icons.Outlined.CalendarMonth,
        "heart-pulse" to Icons.Outlined.Favorite,
        "heart-broken" to Icons.Outlined.HeartBroken,
        "battery-low" to Icons.Outlined.BatteryAlert,
        "battery-alert" to Icons.Outlined.BatteryAlert,
        "brain" to Icons.Outlined.Psychology,
        "smile" to Icons.Outlined.SentimentSatisfied,
        "smile-2" to Icons.Outlined.SentimentVerySatisfied,
        "meh" to Icons.Outlined.SentimentDissatisfied,
        "flame" to Icons.Outlined.LocalFireDepartment,
        "eye-off" to Icons.Outlined.VisibilityOff,
        "thermometer" to Icons.Outlined.Sick,
        "cloud-fog" to Icons.Outlined.Cloud,
        "leaf" to Icons.Outlined.Eco,
        "party-popper" to Icons.Outlined.Celebration,
        "thumb-up" to Icons.Outlined.ThumbUp,
        "meditation" to Icons.Outlined.SelfImprovement,
        "volunteer" to Icons.Outlined.VolunteerActivism,
        "sparkles" to Icons.Outlined.Celebration,
        "auto-awesome" to Icons.Outlined.AutoAwesome,
        "monitor-play" to Icons.Outlined.Movie,
        "droplets" to Icons.Outlined.WaterDrop,
        "dumbbell" to Icons.Outlined.FitnessCenter,
        "wine" to Icons.Outlined.LocalBar,
        "wine-bar" to Icons.Outlined.WineBar,
        "cafe" to Icons.Outlined.LocalCafe,
        "restaurant" to Icons.Outlined.Restaurant,
        "cake" to Icons.Outlined.Cake,
        "icecream" to Icons.Outlined.Icecream,
        "local-drink" to Icons.Outlined.LocalDrink,
        "music-note" to Icons.Outlined.MusicNote,
        "headphones" to Icons.Outlined.Headphones,
        "piano" to Icons.Outlined.Piano,
        "gamepad" to Icons.Outlined.Gamepad,
        "esports" to Icons.Outlined.SportsEsports,
        "tv" to Icons.Outlined.Tv,
        "videocam" to Icons.Outlined.Videocam,
        "camera" to Icons.Outlined.CameraAlt,
        "wrench" to Icons.Outlined.Handyman,
        "cup-soda" to Icons.Outlined.Spa,
        "baby" to Icons.Outlined.Face,
        "toys" to Icons.Outlined.Toys,
        "smart-toy" to Icons.Outlined.SmartToy,
        "gift" to Icons.Outlined.CardGiftcard,
        "shopping-bag" to Icons.Outlined.ShoppingBag,
        "mall" to Icons.Outlined.LocalMall,
        "money" to Icons.Outlined.AttachMoney,
        "diamond" to Icons.Outlined.Diamond,
        "laundry" to Icons.Outlined.LocalLaundryService,
        "iron" to Icons.Outlined.Iron,
        "soap" to Icons.Outlined.Soap,
        "clean-hands" to Icons.Outlined.CleanHands,
        "healing" to Icons.Outlined.Healing,
        "medication" to Icons.Outlined.Medication,
        "hospital" to Icons.Outlined.LocalHospital,
        "kitchen" to Icons.Outlined.Kitchen,
        "lightbulb" to Icons.Outlined.Lightbulb,
        "bolt" to Icons.Outlined.Bolt,
        "car" to Icons.Outlined.DirectionsCar,
        "electric-car" to Icons.Outlined.ElectricCar,
        "taxi" to Icons.Outlined.LocalTaxi,
        "bus" to Icons.Outlined.DirectionsBus,
        "train" to Icons.Outlined.Train,
        "tram" to Icons.Outlined.Tram,
        "flight" to Icons.Outlined.Flight,
        "boat" to Icons.Outlined.DirectionsBoat,
        "sailing" to Icons.Outlined.Sailing,
        "bike" to Icons.Outlined.DirectionsBike,
        "rocket" to Icons.Outlined.RocketLaunch,
        "map" to Icons.Outlined.Map,
        "navigation" to Icons.Outlined.Navigation,
        "public" to Icons.Outlined.Public,
        "language" to Icons.Outlined.Language,
        "person" to Icons.Outlined.Person,
        "groups" to Icons.Outlined.Groups,
        "wc" to Icons.Outlined.Wc,
        "pets" to Icons.Outlined.Pets,
        "lock" to Icons.Outlined.Lock,
        "key" to Icons.Outlined.VpnKey,
        "shield" to Icons.Outlined.Shield,
        "privacy-tip" to Icons.Outlined.PrivacyTip,
        "push-pin" to Icons.Outlined.PushPin,
        "bookmark" to Icons.Outlined.Bookmark,
        "flag" to Icons.Outlined.Flag,
        "star" to Icons.Outlined.Star,
        "trophy" to Icons.Outlined.EmojiEvents,
        "brush" to Icons.Outlined.Brush,
        "palette" to Icons.Outlined.Palette,
        "baseball" to Icons.Outlined.SportsBaseball,
        "soccer" to Icons.Outlined.SportsSoccer,
        "tennis" to Icons.Outlined.SportsTennis,
        "golf" to Icons.Outlined.SportsGolf
    )

    /** 提供给「标签管理」选图标用的候选列表。 */
    val candidates: List<String> = listOf(
        "tag",
        "hash",
        "folder",
        "hand",
        "place",
        "mood",
        "category",
        "star",
        "diamond",
        "flag",
        "bookmark",
        "push-pin",
        "bed",
        "bed-double",
        "king-bed",
        "sofa",
        "event-seat",
        "chair",
        "shower-head",
        "bathtub",
        "hot-tub",
        "door-closed",
        "briefcase",
        "building-2",
        "home",
        "landscape",
        "park",
        "forest",
        "yard",
        "nature",
        "beach",
        "pool",
        "waves",
        "wind",
        "snowflake",
        "clock",
        "sunrise",
        "sun",
        "sunset",
        "moon",
        "moon-star",
        "nightlight",
        "calendar",
        "calendar-days",
        "heart-pulse",
        "heart-broken",
        "battery-alert",
        "brain",
        "smile",
        "smile-2",
        "meh",
        "flame",
        "eye-off",
        "thermometer",
        "cloud-fog",
        "leaf",
        "party-popper",
        "thumb-up",
        "meditation",
        "volunteer",
        "monitor-play",
        "droplets",
        "dumbbell",
        "wine",
        "wine-bar",
        "cafe",
        "restaurant",
        "cake",
        "icecream",
        "local-drink",
        "music-note",
        "headphones",
        "piano",
        "gamepad",
        "esports",
        "tv",
        "videocam",
        "camera",
        "wrench",
        "cup-soda",
        "baby",
        "toys",
        "smart-toy",
        "gift",
        "shopping-bag",
        "mall",
        "money",
        "laundry",
        "iron",
        "soap",
        "clean-hands",
        "healing",
        "medication",
        "hospital",
        "kitchen",
        "lightbulb",
        "bolt",
        "car",
        "electric-car",
        "taxi",
        "bus",
        "train",
        "tram",
        "flight",
        "boat",
        "sailing",
        "bike",
        "rocket",
        "map",
        "navigation",
        "public",
        "language",
        "person",
        "groups",
        "wc",
        "pets",
        "lock",
        "key",
        "shield",
        "privacy-tip",
        "auto-awesome",
        "sparkles",
        "trophy",
        "brush",
        "palette",
        "baseball",
        "soccer",
        "tennis",
        "golf"
    )

    fun iconFor(name: String): ImageVector = map[name.lowercase()] ?: Icons.Outlined.Tag
}
