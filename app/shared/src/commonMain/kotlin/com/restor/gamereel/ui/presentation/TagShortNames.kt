package com.restor.gamereel.ui.presentation

// display names for tags whose raw name is long or technical, keyed by lowercase raw name
internal val tagShortNames: Map<String, String> = buildMap {
    """
    single-room prototype :: One-room prototype
    one mechanic with variations :: One mechanic
    reusable modular levels :: Modular levels
    short self-contained experience :: Short complete game
    small cast of characters :: Small cast
    single environment with changing rules :: One evolving location
    compact handcrafted challenge set :: Handcrafted challenges
    limited tool set with systemic depth :: Few versatile tools
    one-button input :: One button
    touch-first controls :: Touch controls
    portrait orientation :: Portrait screen
    landscape orientation :: Landscape screen
    interruptible short sessions :: Short pausable sessions
    remappable controls :: Remappable controls
    playable without sound :: Play without sound
    offline-first play :: Offline play
    one-time purchase :: One-time purchase
    free complete game :: Fully free
    free demo with full-game unlock :: Demo + full unlock
    optional cosmetic purchases :: Cosmetic purchases
    optional content expansions :: Content expansions
    pay what you want :: Pay what you want

    bird-eye strategy view :: Bird's-eye
    cinematic room camera :: Room camera
    close-follow camera :: Close follow
    fixed cinematic angles :: Fixed angles
    map-only interface :: Map only
    picture-in-picture drone camera :: Drone cam
    2d side scrolling :: Side-scroller
    3d side-on :: 2.5D side
    free rts camera :: RTS camera
    helmet/body camera :: Bodycam
    multi-camera surveillance :: CCTV
    orbital strategy camera :: Orbit camera
    over-the-shoulder :: Over-shoulder
    screen/terminal-only view :: Terminal only
    security-camera view :: Security cam
    split perspective :: Split view
    switchable first/third person :: FPP / TPP
    vehicle chase camera :: Chase cam
    vehicle cockpit :: Cockpit

    disaster response :: Disaster relief
    power-grid restoration :: Grid repair
    rescue operations :: Rescue
    3d platforming :: 3D platformer
    4x strategy | 4x :: 4X
    arcade action :: Arcade
    arena combat :: Arena fights
    assassination planning :: Assassination
    assembly work | assembly :: Assembly
    automation programming :: Automation code
    beat-em-up combat :: Beat 'em up
    board-game rules :: Board game
    board-game strategy :: Board strategy
    card play :: Cards
    cargo handling | cargo logistics :: Cargo
    casual short-session loop :: Casual
    chance-based game loop :: Gambling
    chess/board strategy :: Chess
    choice/narrative reading :: Visual novel
    circuit building :: Circuits
    city building :: City builder
    clicker/incremental :: Clicker
    colony management :: Colony sim
    construction work | construction :: Construction
    conversation choices | dialogue interaction :: Dialogue
    courier routing :: Courier
    courtroom argument :: Courtroom
    creature collection :: Monster catching
    crime investigation | detective mystery :: Detective
    deliberate melee combat :: Tactical melee
    delivery driving :: Delivery
    disassembly work :: Disassembly
    dress-up/customization :: Dress-up
    dungeon crawling :: Dungeon crawl
    economy management | economy simulation :: Economy
    ecosystem management :: Ecosystem
    endless running :: Endless runner
    escape-room puzzles :: Escape room
    exploration/adventure :: Adventure
    factory automation :: Factory
    factory line design :: Line design
    factory operation :: Factory work
    football/soccer :: Football
    grappling traversal :: Grappling
    heist planning :: Heist
    hidden-object searching :: Hidden object
    hotel management :: Hotel sim
    idle progression | idle sessions :: Idle
    incremental progression | incremental growth :: Incremental
    learning mechanics :: Learning
    life simulation :: Life sim
    machine operation :: Machinery
    maintenance work | machine maintenance :: Maintenance
    medical diagnosis :: Diagnosis
    monster hunting :: Monster hunt
    museum curation :: Museum
    music performance :: Music
    object stacking | stacking objects :: Stacking
    packing orders | packing objects :: Packing
    party rpg combat :: Party combat
    party rpg systems :: Party RPG
    physics interaction | physics manipulation :: Physics
    pipeline routing | pipe routing :: Pipes
    political management :: Politics
    power management :: Power
    precision platforming :: Precision platformer
    projectile dodging | dense projectile pressure :: Bullet hell
    puzzle platforming :: Puzzle platformer
    quality inspection :: Inspection
    quiz/knowledge :: Quiz
    railway building :: Railways
    real-time strategy :: RTS
    relationship choices | relationship system :: Relationships
    resource management :: Resources
    restaurant management :: Restaurant
    rhythm play | rhythm timing | rhythm minigames :: Rhythm
    ritual casting :: Rituals
    road building :: Roads
    robot assembly :: Robot building
    robot programming :: Robot coding
    role-playing systems :: RPG
    security monitoring :: Surveillance
    ship management :: Ship sim
    solitaire/card play :: Solitaire
    spacecraft operation :: Spaceflight
    spell combination :: Spell combos
    spell crafting :: Spellcraft
    sports mechanics :: Sports
    submarine piloting :: Submarine
    systemic problem solving :: Systems puzzles
    thieving :: Thievery
    traffic management | traffic planning | traffic routing | traffic congestion :: Traffic
    train operation | train controls :: Train driving
    transport planning :: Transport
    transport/logistics :: Logistics
    treasure hunting :: Treasure hunt
    turn-based combat :: Turn-based
    turn-based strategy :: TBS
    turn-based tactics :: Tactics
    vehicle customization :: Car tuning
    vehicle repair :: Car repair
    vehicular combat :: Car combat
    water management :: Water
    weapon smithing :: Weaponsmith
    word puzzles :: Word games

    ash-covered landscape :: Ashlands
    digital environment :: Cyberspace
    drought landscape :: Drought
    extreme heat :: Heatwave
    flooded landscape :: Floodlands
    giant interior space :: Giant interior
    heavy rain :: Rain
    lake district :: Lakes
    permanent night :: Eternal night
    post-disaster landscape :: Aftermath
    sand reclaiming city :: Sand-buried city
    storm zone :: Storms
    toxic wasteland :: Toxic waste
    tropical coast :: Tropics
    abandoned infrastructure :: Abandoned
    deep underground :: Underground
    dense forest :: Thick forest
    dense urban :: Metropolis
    extreme cold :: Frost
    fog-heavy landscape | fog-heavy :: Fog
    forest reclaiming city :: Overgrown city
    ice reclaiming city :: Frozen city
    liminal spaces :: Liminal
    mars-like desert :: Martian desert
    miniature domestic world :: Tiny house
    orbital exterior :: Orbit
    radioactive wasteland :: Radiation zone
    sand dunes :: Dunes
    space void :: Void
    war-torn landscape :: Warzone

    auction bidding | auction/trading :: Auctions
    cable connection :: Cables
    code breaking :: Codebreaking
    drag-and-drop ui :: Drag & drop
    forklift controls :: Forklift
    gauge balancing :: Gauges
    hose connection :: Hoses
    lever operation :: Levers
    manual cranking :: Cranking
    manual pumping :: Pumping
    plug-and-socket connection :: Plugs
    rope interaction :: Ropes
    sorting objects :: Sorting
    summon placement :: Summoning
    valve turning :: Valves
    wiring circuits | electricity routing :: Wiring
    aimed shooting :: Aiming
    automation logic :: Automation
    board placement :: Board moves
    button/switch panels :: Switch panels
    buy/sell decisions :: Trading
    card placement :: Card placing
    cleaning surfaces :: Cleaning
    combo inputs | combo combat :: Combos
    cooking manipulation | cooking minigame :: Cooking
    crane controls :: Crane
    cutting materials :: Cutting
    destructible environment :: Destruction
    destructible objects :: Breakables
    detective evidence linking :: Evidence board
    direct object grabbing :: Grabbing
    drag and carry :: Carrying
    drawing shapes :: Drawing
    environmental puzzle interaction :: Environment puzzles
    flight controls :: Flying
    fluid pumping | fluid routing :: Fluids
    freeform building :: Freeform build
    gesture input :: Gestures
    grid inventory decisions :: Grid inventory
    hacking minigames :: Hacking
    hidden-role communication :: Bluffing
    inventory decisions | inventory management :: Inventory
    inventory tetris :: Inventory Tetris
    machine calibration :: Calibration
    manual navigation :: Navigation
    manual tool use :: Hand tools
    map annotation :: Map notes
    momentum traversal :: Momentum
    movement mastery :: Movement tech
    multiple-solution interactions :: Many solutions
    painting surfaces :: Painting
    parkour movement | parkour traversal :: Parkour
    parry/dodge timing :: Parry & dodge
    placing objects precisely :: Precise placing
    production scheduling :: Scheduling
    proximity voice interaction | proximity voice :: Proximity chat
    recipe mixing :: Mixing
    remote drone control :: Drones
    repairing components | repair minigames :: Repairs
    resource allocation :: Allocation
    rotational tightening :: Tightening
    route planning :: Routing
    sailing controls :: Sailing
    scanning/inspection :: Scanning
    skiing/skating :: Skiing
    snap-grid building :: Grid building
    stealth takedowns :: Takedowns
    steering heavy vehicles :: Heavy vehicles
    terminal commands :: Terminal
    terrain editing :: Terraforming
    throwing objects :: Throwing
    tile placement :: Tiles
    trap placement :: Traps
    turret placement :: Turrets
    two-player carry :: Co-op carry
    typing commands :: Typing
    user-generated content :: UGC
    vehicle control :: Driving
    vertical traversal :: Verticality
    voxel digging :: Digging
    worker assignment :: Workers

    backstage complex :: Backstage
    bakery plant :: Bakery
    cold storage facility :: Cold storage
    customs terminal :: Customs
    damaged coastal complex :: Coastal ruins
    desert convoy stop :: Convoy stop
    desert research camp :: Desert camp
    disaster shelter :: Shelter
    distribution center :: Distribution hub
    food processing plant :: Food plant
    giant tree interior :: Giant tree
    greenhouse complex :: Greenhouses
    highway interchange :: Interchange
    highway rest stop :: Rest stop
    hydroelectric dam :: Dam
    industrial district :: Industrial zone
    inside a computer :: Inside a PC
    inside a creature :: Inside a beast
    inside a machine :: Inside a machine
    open-pit quarry :: Quarry
    radio tower complex :: Radio tower
    roadside diner :: Diner
    suburban neighborhood :: Suburbs
    subway station :: Subway
    train-city :: Train city
    treehouse settlement :: Treehouses
    tunnel network :: Tunnels
    underground utility tunnels :: Utility tunnels
    university campus :: Campus
    volcano facility :: Volcano lab
    water treatment plant :: Water plant
    liminal office complex :: Liminal office
    megacity block :: Megablock
    nuclear-style power complex :: Nuclear plant
    particle accelerator :: Collider
    planetary colony :: Colony

    3-player co-op :: 3P co-op
    6-player co-op :: 6P co-op
    8-player co-op :: 8P co-op
    2-player co-op :: 2P co-op
    4-player co-op :: 4P co-op
    4-player local :: 4P local
    co-op with separate roles :: Role co-op
    co-op with shared resources :: Shared co-op
    drop-in/drop-out co-op :: Drop-in co-op
    free-for-all pvp :: FFA PvP
    hot-seat multiplayer :: Hot-seat
    persistent multiplayer :: Persistent online
    solo or co-op :: Solo / co-op
    solo with ai companions :: AI squad
    spectator-supported multiplayer :: Spectators
    1-vs-many :: 1 vs many
    asymmetric multiplayer | asymmetric rounds :: Asymmetric
    co-op with friendly fire :: Friendly fire
    competitive multiplayer :: Competitive
    cooperative/ai opposition :: Co-op vs AI
    hidden-role multiplayer :: Hidden roles
    local multiplayer :: Couch play
    massively multiplayer :: MMO
    offline/singleplayer-friendly :: Offline
    online multiplayer :: Online
    party multiplayer :: Party game
    social-deduction multiplayer :: Social deduction
    split-screen multiplayer :: Split-screen
    team pvp :: Team PvP
    pvp :: PvP
    pve :: PvE

    choose your own adventure :: CYOA
    open world survival craft :: Survival craft
    football (american) :: American football
    football (soccer) :: Soccer
    based on a novel :: Book-based
    real-time with pause :: Real-time pause
    asynchronous multiplayer :: Async MP
    grid-based movement :: Grid movement
    lgbtq+ :: LGBTQ+
    moba :: MOBA
    rpgmaker :: RPG Maker
    artificial intelligence :: AI
    dungeons & dragons :: D&D
    warhammer 40k :: Warhammer 40K
    games workshop :: Games Workshop
    traditional roguelike :: Roguelike
    trading card game :: TCG
    real time tactics :: RTT
    2d fighter :: 2D fighter
    3d fighter :: 3D fighter
    2d platformer :: 2D platformer
    action rts :: Action RTS
    villain protagonist :: Play as villain
    silent protagonist :: Silent hero
    walking simulator :: Walking sim
    job simulator :: Job sim
    automobile sim :: Car sim
    top-down shooter :: Twin-stick shooter
    party-based rpg :: Party RPG
    strategy rpg :: Strategy RPG
    tactical rpg :: Tactical RPG
    mars :: Mars
    rome :: Rome
    lego :: LEGO

    one-way progression :: No going back
    quality degradation :: Degradation
    rising water :: Flooding
    ammo scarcity :: Low ammo
    body recovery risk | body recovery :: Corpse run
    cold exposure :: Cold
    communication limits :: Comms limits
    countdown timer :: Countdown
    customer queue :: Queues
    day/night danger shift | nightfall danger :: Deadly nights
    deadline/contracts | delivery deadlines :: Deadlines
    debt/quota pressure :: Quota
    detection avoidance :: Stay hidden
    disease risk | disease system :: Disease
    economic upkeep | maintenance costs :: Upkeep
    escalating enemy presence :: Rising threat
    escalating waves | wave-based sessions :: Waves
    food scarcity :: Food shortage
    fuel scarcity :: Low fuel
    fuel management :: Fuel
    hardcore punishment :: Hardcore
    heat buildup | overheating systems :: Overheating
    high difficulty :: Hard
    high punishment :: Punishing
    horror threat :: Horror
    hunger/thirst :: Hunger
    infection spread :: Infection
    information scarcity :: Unknowns
    injury accumulation | injury system :: Injuries
    inventory pressure :: Full bags
    last-player pressure :: Last standing
    law enforcement response :: Police
    limited carry slots :: Few slots
    limited crew capacity :: Small crew
    limited moves :: Move limit
    limited power :: Low power
    limited safe areas :: Rare safety
    limited storage :: Storage limit
    limited turns :: Turn limit
    lose carried loot on death :: Loot loss
    machine breakdown risk :: Breakdowns
    market fluctuations :: Volatile market
    morale collapse :: Morale
    moving hazard zone :: Moving hazard
    no-save sections :: No saves
    noise attracts danger :: Noise danger
    objective escalation :: Rising stakes
    optional greed vs safety :: Greed vs safety
    oxygen scarcity :: Oxygen
    patrol routes :: Patrols
    player competition :: Rivals
    precision execution :: Precision
    production backlog :: Backlog
    projectile avoidance :: Dodging
    pursuer pressure :: Pursuit
    radiation exposure :: Radiation
    rent/taxes :: Rent
    reputation risk | reputation system | reputation progression :: Reputation
    resource scarcity | scarce resources :: Scarcity
    risk/reward extraction | extraction runs :: Extraction
    route uncertainty :: Unknown route
    sanity/stress :: Sanity
    save-point scarcity :: Rare saves
    separation risk :: Separation
    shrinking safe zone :: Shrinking zone
    shrinking/last-player pressure | battle-royale matches :: Battle royale
    spreading fire :: Fire
    stalking enemy :: Stalker
    stealth detection :: Detection
    storm approaching :: Incoming storm
    structural collapse risk :: Collapse
    survival needs :: Survival
    suspicion meter :: Suspicion
    time pressure :: Time limit
    toxic atmosphere :: Toxic air
    unstable power grid :: Blackouts
    visibility/detection pressure | visibility system :: Visibility
    water scarcity :: Thirst
    weather deterioration :: Bad weather
    weight pressure | weight limits :: Weight

    branching unlocks :: Unlock tree
    facility restoration :: Restoration
    ship/base restoration :: Base repair
    ability-gated progression :: Ability gates
    achievement-based unlocks :: Achievements
    automation scaling :: Scaling up
    blueprint progression | blueprint discovery :: Blueprints
    build customization :: Builds
    business growth :: Business
    carry-capacity upgrades :: Bigger bags
    character stat progression :: Stats
    checkpoint progression :: Checkpoints
    class/build progression :: Classes
    collection completion :: Completionism
    collection progression :: Collecting
    companion progression | companion management :: Companions
    cosmetic collection | cosmetics collection :: Cosmetics
    crafting mastery :: Craft mastery
    crafting quality mastery :: Craft quality
    crew progression | crew management :: Crew
    difficulty tier progression :: Difficulty tiers
    empire progression :: Empire
    equipment progression :: Gear
    faction progression | faction relations :: Factions
    factory/system scaling :: Factory scaling
    gear-score progression :: Gear score
    hero/ability progression :: Hero abilities
    inventory expansion :: More slots
    knowledge progression :: Knowledge
    legacy/generational progression :: Legacy
    level-based progression | handcrafted levels :: Levels
    long-form empire progression :: Empire saga
    loot-driven progression :: Loot
    map knowledge progression :: Map knowledge
    mobile-base expansion | mobile base progression :: Mobile base
    money-based upgrades :: Cash upgrades
    new biome unlocks :: New biomes
    new mission tier unlocks :: Mission tiers
    new route unlocks :: New routes
    permadeath inheritance :: Inheritance
    pet progression | pet system :: Pets
    player-skill-only progression :: Skill only
    prestige/reset progression :: Prestige
    profession mastery :: Professions
    rank/league progression :: Ranked
    recipe discovery | crafting recipes :: Recipes
    relationship progression :: Bonds
    research progression :: Research
    resource-investment upgrades :: Upgrades
    rpg progression :: RPG leveling
    run deck growth :: Run deck
    run reset/progression :: Run resets
    season progression | season cycle | seasonal cycle :: Seasons
    shortcut unlocks :: Shortcuts
    skill-tree progression :: Skill tree
    story unlock progression :: Story unlocks
    team composition progression :: Team building
    technology tree | research tree :: Tech tree
    territory expansion :: Territory
    tool upgrade progression :: Tool upgrades
    unlock progression :: Unlocks
    vehicle upgrade progression :: Vehicle upgrades
    weapon upgrade progression :: Weapon upgrades
    world restoration :: Restoring world

    cannibalization :: Parts cannibalizing
    base decoration | home decoration :: Decorating
    branching dialogue :: Dialogue tree
    building and crafting :: Build & craft
    character customization :: Character editor
    codex discovery :: Codex
    crafting minigame :: Crafting
    daily tasks :: Dailies
    day/night cycle :: Day & night
    delivery contracts :: Deliveries
    dynamic weather :: Weather
    emergent incidents :: Incidents
    environmental storytelling :: Environmental story
    equipment loadouts :: Loadouts
    fire propagation :: Fire spread
    fishing minigame :: Fishing
    grappling-hook traversal :: Grappling hook
    level creation :: Level editor
    loot collection :: Looting
    map drawing :: Mapping
    meaningful choices :: Choices
    modding support :: Mods
    morality choices :: Morality
    mount system :: Mounts
    noise system :: Noise
    order fulfillment :: Orders
    party composition :: Party
    permadeath mode :: Permadeath
    procedural generation | procedurally varied content :: Procgen
    quests/contracts :: Quests
    quick-time events :: QTE
    resource extraction :: Gathering
    revive system :: Revives
    salvage collection :: Salvage
    secret hunting :: Secrets
    shift planning | work shifts :: Shifts
    shop management :: Shop
    skill loadouts :: Skills
    stamina management :: Stamina
    stealth gadgets :: Gadgets
    swimming/diving :: Diving
    technology unlocks :: Tech unlocks
    temperature system :: Temperature
    temporary repairs :: Quick fixes
    time management :: Time
    train scheduling :: Train schedules
    vehicle maintenance :: Car upkeep
    vehicle ownership :: Own vehicle
    water simulation :: Water physics
    weapon customization :: Gunsmith
    weapon durability :: Durability

    colonial frontier | frontier settlement | frontier civilization | frontier adventure :: Frontier
    interstellar colony :: Star colony
    monster society :: Monsters
    nomadic society | nomadic world :: Nomads
    ocean civilization :: Ocean world
    polar expedition :: Polar
    research expedition :: Expedition
    university life :: University
    virtual world :: Virtual
    ancient world :: Antiquity
    animal society | animal civilization :: Animals
    archaeological expedition :: Archaeology
    caravan world :: Caravans
    corporate world :: Corporate
    crime/underworld | crime underworld :: Underworld
    deep-sea expedition :: Deep sea
    dragons/fantasy :: Dragons
    giant-scale world :: Giant world
    industrial revolution :: Industrial age
    isolated community | isolated society :: Isolation
    magical academy :: Magic school
    modern america :: Modern USA
    occult conspiracy | occult mystery :: Occult
    orbital habitat :: Orbital
    post-apocalyptic :: Post-apoc
    post-collapse civilization :: Post-collapse
    post-human world | post-human future | post-human identity :: Post-human
    restored world :: Restoration
    retrofuturistic :: Retro-future
    robot civilization :: Robots
    ruined civilization :: Ruins
    samurai era :: Samurai
    science fiction :: Sci-fi
    science/technology :: Science
    secret government facility :: Secret lab
    simulation world :: Simulation
    small-town life | small-town contemporary :: Small town
    time-travel setting :: Time travel
    underground civilization :: Underground
    wilderness/nature :: Wilderness
    world war i :: WWI
    world war ii :: WWII
    zombie apocalypse :: Zombies

    factory shift loop :: Factory shifts
    arena matches :: Arena
    boss-rush structure :: Boss rush
    build-defend-expand loop :: Build & defend
    campaign with new game+ :: NG+
    caravan journey :: Caravan
    chapter structure :: Chapters
    day/job/day loop :: Day job
    delivery route loop :: Delivery routes
    endless score run :: Endless score
    episodic campaign :: Episodic
    expedition runs :: Expeditions
    explore-gather-build loop :: Explore & build
    floor-by-floor descent :: Descent
    generational progression :: Generations
    heist planning/execution loop :: Heists
    hub-and-mission :: Hub & missions
    interconnected gated world :: Gated world
    investigate-decide-consequence loop :: Investigations
    journey across map :: Journey
    linear campaign :: Linear
    management calendar :: Calendar
    metroidvania world :: Metroidvania
    mission-based campaign :: Missions
    multiple parallel objectives :: Multitasking
    narrative campaign :: Story campaign
    one central base plus excursions :: Base & sorties
    open-ended life loop :: Life sandbox
    persistent colony timeline :: Colony timeline
    persistent online world :: Online world
    player-built world :: Player-built
    prepare-execute-return loop :: Prep & return
    prepare-extract-upgrade loop :: Extract & upgrade
    procedural labyrinth :: Labyrinth
    real-time continuous simulation :: Real-time sim
    road-trip structure :: Road trip
    roguelike runs :: Roguelike
    roguelite runs :: Roguelite
    room-by-room crawl :: Room crawl
    round-based matches :: Rounds
    run-based meta progression :: Meta runs
    score-attack sessions :: Score attack
    seasonal management :: Seasonal sim
    semi-open hubs :: Hubs
    semi-random map sectors :: Random sectors
    shop open/close loop :: Shop days
    short arcade stages :: Arcade stages
    short contract runs :: Contracts
    short party rounds :: Party rounds
    short score-based sessions :: Score runs
    single-location evolving map | whole campaign happens in one evolving location :: One location
    survival endurance :: Endurance
    time-trial sessions :: Time trials
    tower ascent :: Tower climb
    travel-stop-travel loop :: Road stops

    capitalism satire :: Anti-capitalism
    community building :: Community
    ecological collapse :: Eco collapse
    fairy-tale wonder :: Fairy tale
    industrial grime :: Grime
    retro nostalgia :: Nostalgia
    small-scale everyday life :: Everyday life
    technology anxiety :: Tech anxiety
    absurd comedy :: Absurdism
    ai anxiety :: AI anxiety
    chaotic co-op :: Co-op chaos
    corporate absurdity :: Corporate absurd
    dark fairy tale :: Dark tale
    heist fantasy :: Heist
    mundane work made dangerous | ordinary job becomes dangerous :: Deadly job
    pirate adventure | pirate age :: Pirates
    political intrigue :: Intrigue
    psychological horror :: Psych horror
    risk/reward :: Risk & reward
    workplace comedy :: Office comedy

    all abilities come from items :: Item powers
    base can be lost permanently :: Losable base
    best reward requires making situation less safe :: Risk for reward
    boss is an environmental system rather than creature :: System boss
    building rearranges during play :: Shifting building
    city itself is a moving machine :: Walking city
    correct solution is often slower than risky shortcut :: Slow is safe
    crafting happens by assembling real pieces :: Real assembly
    danger follows valuable resources :: Cursed loot
    darkness changes level readability :: Dark shifts
    dialogue happens during active gameplay :: Talk on the go
    enemies are also useful resources :: Enemies as loot
    entire game happens on a moving train :: All on a train
    every enemy has one unique rule :: Rule enemies
    every tool is also a weapon :: Tool = weapon
    every weapon is also a tool :: Weapon = tool
    factory is also the labyrinth :: Factory maze
    failure creates new opportunities :: Fail forward
    final objective is simply getting home :: Get home
    game begins after the apocalypse is already old :: Old apocalypse
    inventory is physically represented in the world :: Physical inventory
    inventory opens as physical object :: Diegetic bag
    large rooms are local mazes themselves :: Room mazes
    level is one giant machine :: Level machine
    loot can invalidate completed objectives :: Loot backfires
    main objective can be dismantled for parts :: Scrap the goal
    management decisions happen while action continues :: Live management
    map changes because of player systems :: Reactive map
    money is hidden until the end of the run :: Hidden money
    no permanent character upgrades :: No upgrades
    no teleport between exterior and interior :: Seamless interiors
    not every mystery has a reward :: Empty mysteries
    nothing is automatically extracted :: Manual extraction
    one player controls logistics while others act physically :: Dispatcher role
    only the base progresses :: Base-only growth
    only the world progresses :: World-only growth
    optional work can be more valuable than mission objective :: Side jobs pay
    player can finish task using parts stolen from optional systems :: Borrowed parts
    player chooses when to wake the facility :: Wake the facility
    player inherits previous crews mistakes :: Inherited mess
    players can intentionally cause disasters for profit :: Profitable disasters
    players can split but communication becomes harder :: Split up
    players decide when run is over :: You call it
    powering lights also powers threats :: Light wakes threats
    previous failed runs leave physical traces :: Ghost runs
    quests are generated from current world state :: Living quests
    resources are other machines/components :: Machine parts
    restoring infrastructure opens new hazards :: Repair hazards
    roles are defined only by equipment :: Gear = role
    routes change after accidents :: Shifting routes
    safe area slowly disappears :: Shrinking haven
    safe-looking rooms can become dangerous after blackout :: Blackout danger
    secondary exits can emerge far from the vehicle :: Far exits
    shop remains open during disasters :: Open in chaos
    some dead ends contain absolutely nothing :: Empty dead ends
    team can leave at almost any time :: Leave anytime
    temporary repair may fail later :: Fragile fixes
    the game takes place during one long shift :: One long shift
    the map is physically continuous from base to dungeon :: Seamless map
    the safest route gives the smallest reward :: Safe = poor
    turning systems on makes map more dangerous :: Power = danger
    vehicle is the extraction boundary :: Vehicle exit
    victory increases future obligations :: Costly victory
    world generation uses real functional dependencies :: Logical world
    world has no traditional enemies :: No enemies
    world moves while player stays put :: Moving world
    you can steal parts from working systems :: Steal parts
    you know the goal but not the exact reward :: Mystery reward
    base is also the main vehicle :: Base on wheels
    camera is a game mechanic :: Camera mechanic
    death advances the story :: Death = story
    debt replaces traditional experience points :: Debt as XP
    enemies learn player habits :: Adaptive enemies
    enemy cannot be killed but can be redirected :: Unkillable foe
    everything valuable must be physically carried :: Carry it all
    game deliberately breaks its own interface :: Broken UI
    light is a limited resource :: Scarce light
    map has no minimap but uses strong landmarks :: Landmarks only
    one object is the center of the whole game :: One object
    one player has different information from others :: Secret info
    player manages a place but performs all work manually :: Hands-on manager
    player repairs the world instead of conquering it :: Repair, not conquer
    player works for an organization they never see :: Faceless employer
    players create most of the disasters themselves :: Self-made chaos
    players draw their own map :: Draw the map
    players see different versions of reality :: Split reality
    progress comes only from knowledge :: Knowledge only
    progress resets but knowledge remains :: Time loop
    sound is the primary navigation tool :: Navigate by sound
    the environment is the enemy :: Hostile nature
    the ui exists physically in the world :: Diegetic UI
    time never pauses even in menus :: No pause
    time only advances when player acts :: Time moves with you
    two gameplay loops feed each other :: Twin loops
    world reacts to noise physically :: Loud world

    biopunk visual :: Biopunk
    comic halftone :: Halftone
    dark low-visibility :: Pitch dark
    dirty utilitarian :: Utilitarian
    flat-shaded :: Flat shaded
    hand-painted 3d :: Painted 3D
    high-contrast lighting :: High contrast
    isometric diorama :: Iso diorama
    low-resolution 3d :: Low-res 3D
    mixed 2d/3d :: 2D + 3D
    modular industrial :: Industrial
    n64-style 3d :: N64 style
    oil-painting look :: Oil painting
    photogrammetry-inspired :: Photogrammetry
    pixelated 3d :: Pixel 3D
    plastic toy-like :: Toy-like
    ps2-era 3d :: PS2 style
    rusty industrial :: Rust
    soft cozy lighting :: Cozy light
    vhs-filtered :: VHS
    1-bit pixel art :: 1-bit
    anime stylized :: Anime
    ascii-like :: ASCII
    black-and-white :: B&W
    bright colorful :: Colorful
    cartoon stylized :: Cartoon
    cassette-futurist :: Cassette future
    cinematic presentation :: Cinematic
    clay-like :: Claymation
    comic-book :: Comic
    crt-filtered :: CRT
    cute/chibi :: Chibi
    dark fantasy visual :: Dark fantasy
    full-motion video :: FMV
    high-res pixel art :: HD pixel art
    low-poly 3d :: Low-poly
    miniature diorama :: Diorama
    pre-rendered backgrounds :: Pre-rendered
    ps1-style 3d :: PS1 style
    solarpunk visual :: Solarpunk
    steampunk visual :: Steampunk
    voxel 3d :: Voxel
    voxel/blocky 3d :: Blocky
    silhouette art :: Silhouettes

    generation-ship society :: Generation ship
    virtual reality :: VR
    workplace contemporary :: Workplace
    abandoned civilization :: Lost civilization
    dream reality :: Dreams
    floating-island civilization :: Sky islands
    interstellar empire :: Star empire
    magic-and-technology :: Magitech
    military conflict :: War
    mysterious ancient civilization :: Ancients
    new colony world :: New colony
    school contemporary :: School
    superhero world :: Superheroes
    world being rebuilt :: Rebuilding
    """.trimIndent().lineSequence().filter { it.isNotBlank() }.forEach { row ->
        val (names, short) = row.split(" :: ", limit = 2)
        names.split(" | ").forEach { put(it.lowercase(), short) }
    }
}
