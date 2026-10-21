# Мобильные сборки GameReel 1.2.0

Локальная `assembleRelease` сохраняет существующий арт и debug-подпись для обновления
установленной тестовой версии. Это не артефакт для публикации.

Отдельная конфигурация:

```powershell
.\gradlew.bat :app:shared:clean :app:androidApp:clean -PplayRelease=true
.\gradlew.bat :app:androidApp:bundlePlay -PplayRelease=true
python tools/check_play_artifact.py app/androidApp/build/play/outputs/bundle/play/androidApp-play.aab
```

При `playRelease=true` Compose Resources берёт подготовленную копию ресурсов без
`files/art/**` и без исторических `steam_broad.json`, `steam_alltime.json`,
`google_play.json`. Загрузчик оставляет редакторский набор и небольшие подборки
itch.io и Steam. Маркер `no-store-art` также отключает поиск и загрузку обложек в UI:
используются значки тегов. Исходники картинок сохранены для локальной версии, но в
Preview/Play APK/AAB они не входят. Подборки не включают картинки. Это уменьшает конкретный риск арта, но не подтверждает права
на все исторические датасеты и не гарантирует одобрение Google.

Без предоставленного upload key AAB остаётся неподписанным и предназначен для
проверки содержимого. Конфигурация Play никогда не использует debug key. Для подписи
задать окружение в своей системе, не коммитить секреты:

- `GAMEREEL_UPLOAD_STORE` — путь к хранилищу ключа;
- `GAMEREEL_UPLOAD_STORE_PASSWORD` — пароль хранилища;
- `GAMEREEL_UPLOAD_KEY_ALIAS` — alias;
- `GAMEREEL_UPLOAD_KEY_PASSWORD` — пароль ключа.

Включить Play App Signing в Console. Не менять applicationId или ключ уже
опубликованного приложения без отдельного плана миграции.

Для проверки интерфейса без арта рядом с локальным приложением:

```powershell
.\gradlew.bat :app:androidApp:assemblePreview -PplayRelease=true
```

Обычная последующая сборка без свойства возвращает локальные ресурсы.
Выходные файлы Play изолированы в `app/shared/build/play` и `app/androidApp/build/play`,
чтобы инкрементальная сборка не переносила старый арт между конфигурациями. Проверка
`check_play_artifact.py` должна отклонить обычный APK с обложками. Она проверяет
отсутствие файлов арта, маркер и ELF-выравнивание. ZIP-выравнивание APK проверить
через Android SDK `zipalign -c -P 16 -v 4 <apk>`, AAB — через bundletool/Console
и запуском с 16 KB страницами памяти.

Перед публикацией ещё нужны: контакты разработчика и публичная политика
([проект](PRIVACY_POLICY_DRAFT.md)), Data safety, возрастная анкета/аудитория,
подтверждение условий использования исходных данных, карточка магазина,
проверенный аккаунт и требуемое закрытое тестирование. Наличие внутреннего экрана
«Конфиденциальность и данные» не заменяет публичный URL и реквизиты.

## Одна команда

```powershell
.\tools\build_mobile.ps1 -Variant All
# Когда upload key уже настроен локально:
.\tools\build_mobile.ps1 -Variant Play -RequireUploadKey
```

- С картинками: `app/androidApp/build/outputs/apk/release/androidApp-release.apk`.
- Preview: `app/androidApp/build/play/outputs/apk/preview/androidApp-preview.apk`.
- Play: `app/androidApp/build/play/outputs/bundle/play/androidApp-play.aab`.

Preview имеет applicationId `com.restor.gamereel.preview` и отдельные данные.
Local и Play используют `com.restor.gamereel`, но разные ключи подписи: локальный APK
не обновляется магазинной версией без совместимого сертификата. Не удалять локальную
установку ради Play до решения вопроса переноса пользовательских данных.

Карточка магазина: [STORE_LISTING.md](STORE_LISTING.md).
Статус и незавершённые условия публикации: [RELEASE_1.2.0.md](RELEASE_1.2.0.md).

Перед публичной сборкой очищаются только изолированные `build/play`: это обязательно
после изменения списка разрешённых ресурсов, поскольку промежуточные Copy-задачи
Compose могут сохранить ранее скопированные файлы. `build_mobile.ps1` делает это
автоматически. Проверка готового артефакта остаётся обязательной.
