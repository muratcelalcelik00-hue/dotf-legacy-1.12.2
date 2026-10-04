# Dawn of the Flood Legacy (Minecraft 1.12.2)

[English](#english) · [Türkçe](#türkçe)

---

## English

**Dawn of the Flood Legacy** (`dotflegacy`) is a Minecraft Forge **1.12.2** backport of the
**Pod Infector** mob from [Dawn of the Flood](https://www.curseforge.com/minecraft/mc-mods/dawn-of-the-flood)
(originally for 1.20.1) by **ASEStefan** and **TeamAbyssal**.

### Features

* `dotflegacy:pod_infector` – a small, fast, wall-climbing hostile mob rendered with GeckoLib.
  * **Pack movement** – patrol leaders pick a far-away patrol target and the pods around them follow;
    nearby pods share attack targets.
  * **Leaps** at its target when it is 2.5–16 blocks away.
  * **Latches onto its victim** (rides it), bites periodically and finally **bursts** into a poison cloud.
  * **Pops on death** (sound + particles, no damage) and makes nearby pods with the same target pop too.
  * Idle / walk / target / swim / air / burrow animations from the original `pod_infector.animation.json`.
* Spawn egg, natural spawning in non-snowy overworld land biomes, loot table (`pod_fragment`), creative tab.
* Languages: English (`en_us`) and Turkish (`tr_tr`).

### Requirements

* Minecraft 1.12.2, Forge 14.23.5.2847+
* [GeckoLib](https://www.curseforge.com/minecraft/mc-mods/geckolib) for 1.12.2 (`geckolib3`, 3.0.26+)

### Building

The project uses [RetroFuturaGradle](https://github.com/GTNewHorizons/RetroFuturaGradle) via the
[CleanroomMC TemplateDevEnv](https://github.com/CleanroomMC/TemplateDevEnv). Gradle runs on **JDK 25**
(RetroFuturaGradle 2.x requires Java 25 to run Gradle itself) and automatically provisions a **Java 8** toolchain for compiling the mod.

```bash
./gradlew build
# -> build/libs/dotflegacy-<version>.jar
```

Useful tasks: `./gradlew runClient`, `./gradlew runServer`.

GeckoLib is pulled from the official GeckoLib Cloudsmith maven and deobfuscated at build time:

```groovy
implementation rfg.deobf('software.bernie.geckolib:geckolib-forge-1.12.2:3.0.31')
```

Every push and pull request is built by GitHub Actions (`.github/workflows/build.yml`); the resulting jar is
uploaded as the `dotflegacy-1.12.2` artifact.

> The workflow file is provided as [`ci/build.yml`](ci/build.yml). The automation that opened the initial PR has no
> `workflows` permission on GitHub, so it could not push `.github/workflows/build.yml` itself. Copy `ci/build.yml`
> to `.github/workflows/build.yml` (e.g. via the GitHub web UI) to enable CI.

### Asset notes

* `pod_infector.geo.json` (format 1.12.0) is used unmodified.
* `pod_infector.animation.json` was converted with `tools/convert_animation.py`: GeckoLib 3.0.x for 1.12.2
  cannot read the newer `{"post": {"vector": [...]}, "lerp_mode": "catmullrom"}` keyframe form, so those
  keyframes were flattened to `{"vector": [...]}` (Catmull-Rom interpolation becomes linear).
* Sounds (`pod_infector.idle1-10`, `pod_infector.pop1-4`, `pod.bite`) and the texture are used as-is.

### License & credits

This project is released under the **MIT License** (see [LICENSE](LICENSE)).

The original Dawn of the Flood mod – including the Pod Infector model, texture, animations and sounds reused
here – is © **ASEStefan** and **TeamAbyssal** and is licensed under the **MIT License**. Huge thanks to them
for creating the mod and releasing it under a permissive license. This port is an unofficial community project
and is not affiliated with the original authors.

---

## Türkçe

**Dawn of the Flood Legacy** (`dotflegacy`), **ASEStefan** ve **TeamAbyssal** tarafından geliştirilen
[Dawn of the Flood](https://www.curseforge.com/minecraft/mc-mods/dawn-of-the-flood) modundaki (1.20.1)
**Pod Infector** yaratığının Minecraft Forge **1.12.2** sürümüne taşınmış hâlidir.

### Özellikler

* `dotflegacy:pod_infector` – GeckoLib ile çizilen küçük, hızlı, duvara tırmanabilen düşman yaratık.
  * **Sürü hareketi** – devriye liderleri uzak bir hedef seçer, çevredeki pod'lar onu takip eder; yakındaki
    pod'lar saldırı hedefini paylaşır.
  * Hedef 2,5–16 blok uzaktayken **üzerine sıçrar**.
  * Kurbanının **üstüne yapışır** (ona biner), periyodik olarak ısırır ve sonunda zehir bulutu bırakarak **patlar**.
  * **Öldüğünde patlar** (ses + partikül, hasar yok) ve aynı hedefe sahip yakındaki pod'ları da patlatır.
  * Orijinal `pod_infector.animation.json` dosyasındaki idle / walk / target / swim / air / burrow animasyonları.
* Yaratma yumurtası, karlı olmayan yüzey biyomlarında doğal spawn, loot table (`pod_fragment`), yaratıcı mod sekmesi.
* Diller: İngilizce (`en_us`) ve Türkçe (`tr_tr`).

### Gereksinimler

* Minecraft 1.12.2, Forge 14.23.5.2847+
* 1.12.2 için [GeckoLib](https://www.curseforge.com/minecraft/mc-mods/geckolib) (`geckolib3`, 3.0.26+)

### Derleme

Proje, [CleanroomMC TemplateDevEnv](https://github.com/CleanroomMC/TemplateDevEnv) üzerinden
[RetroFuturaGradle](https://github.com/GTNewHorizons/RetroFuturaGradle) kullanır. Gradle **JDK 25**
ile çalışır (RetroFuturaGradle 2.x Gradle için Java 25 gerektirir); modu derlemek için gereken **Java 8** toolchain'i otomatik indirilir.

```bash
./gradlew build
# -> build/libs/dotflegacy-<sürüm>.jar
```

Her push ve pull request GitHub Actions ile derlenir (`.github/workflows/build.yml`); üretilen jar
`dotflegacy-1.12.2` adlı artifact olarak yüklenir.

> Workflow dosyası [`ci/build.yml`](ci/build.yml) olarak sunulmuştur. İlk PR'ı açan otomasyonun GitHub'da
> `workflows` izni olmadığı için `.github/workflows/build.yml` dosyasını kendisi push edemedi. CI'ı etkinleştirmek
> için `ci/build.yml` dosyasını `.github/workflows/build.yml` yoluna kopyalayın (ör. GitHub web arayüzünden).

### Asset notları

* `pod_infector.geo.json` (format 1.12.0) değiştirilmeden kullanıldı.
* `pod_infector.animation.json`, `tools/convert_animation.py` ile dönüştürüldü: 1.12.2 için GeckoLib 3.0.x,
  yeni `{"post": {"vector": [...]}, "lerp_mode": "catmullrom"}` keyframe biçimini okuyamadığından bu
  keyframe'ler `{"vector": [...]}` biçimine düzleştirildi (Catmull-Rom yerine doğrusal ara değerleme yapılır).
* Sesler (`pod_infector.idle1-10`, `pod_infector.pop1-4`, `pod.bite`) ve doku olduğu gibi kullanıldı.

### Lisans ve teşekkür

Bu proje **MIT Lisansı** ile yayımlanmıştır (bkz. [LICENSE](LICENSE)).

Burada yeniden kullanılan Pod Infector modeli, dokusu, animasyonları ve sesleri dâhil olmak üzere orijinal
Dawn of the Flood modu © **ASEStefan** ve **TeamAbyssal**'a aittir ve **MIT Lisansı** altındadır. Modu
geliştirip özgür bir lisansla yayımladıkları için kendilerine çok teşekkür ederiz. Bu port resmî olmayan bir
topluluk projesidir; orijinal yazarlarla bağlantılı değildir.
