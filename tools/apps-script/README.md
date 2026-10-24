# Приём паков из приложения

`submit.gs` это Google Apps Script. Приложение отправляет на него пак, скрипт проверяет его,
сохраняет в папку **GameReel submissions** на Google Диске владельца и присылает письмо.
В репозиторий сам по себе ничего не попадает.

## Проверка пака

В письме: название, барабаны, число вариантов в каждой категории, комментарий, сам пак во
вложении и две ссылки.

- **Review and publish**: страница с кнопкой «Open pull request». Скрипт создаёт ветку
  `submissions/<id>-<время>` с файлом `community/packs/<id>.json`, открывает PR с меткой
  `pack-submission`. Workflow `community.yml` проверяет PR, после merge сам пересобирает
  `community/index.json`. Повторное нажатие вернёт уже открытый PR.
- **File on Drive**: файл на Диске. Это готовый пак: можно скачать, положить в
  `community/packs/`, запустить `python tools/validate_community.py` и закоммитить вручную.

Чтобы отклонить пак, удалите файл с Диска. Ссылка из письма содержит секретный ключ
(`REVIEW_KEY` в свойствах скрипта, создаётся сам), письма не пересылайте.

## Токен GitHub

Нужен только для кнопки «Open pull request»; без него паки всё равно сохраняются и приходят письма.

1. GitHub → Settings → Developer settings → Personal access tokens → **Fine-grained tokens** →
   Generate new token.
2. Repository access: **Only select repositories** → `GameReel`.
3. Permissions → Repository: **Contents: Read and write**, **Pull requests: Read and write**
   (Metadata: Read-only добавится сам). Если нужна метка, ещё **Issues: Read and write**.
4. Срок действия: до года; потом токен нужно перевыпустить и заменить в свойствах скрипта.

## Скрипт

1. Открыть <https://script.google.com> → **Новый проект**, назвать `GameReel submissions`.
2. Заменить содержимое `Code.gs` текстом из `submit.gs`, сохранить.
3. ⚙️ **Настройки проекта** → **Свойства скрипта** → добавить `NOTIFY_EMAIL` (куда слать письма) и `GITHUB_TOKEN` со значением токена.
4. **Начать развертывание** → **Новое развертывание** → тип **Веб-приложение**:
   - Выполнять от имени: **От моего имени**;
   - У кого есть доступ: **Все**.
5. Выбрать функцию `authorize` и нажать ▶ «Выполнить»: Google спросит про Диск, отправку почты и внешние запросы (api.github.com).
6. Скопировать URL веб-приложения (`https://script.google.com/macros/s/.../exec`) и вставить в
   `SUBMIT_URL` в `app/shared/src/commonMain/kotlin/com/restor/gamereel/feature/library/PackSubmission.kt`.
   Пока `SUBMIT_URL` пустой, кнопки «Предложить в каталог» в приложении нет.

Проверка: открыть URL в браузере, ответ должен быть `{"ok":true,"service":"gamereel-submissions"}`.

При изменении `submit.gs` нужно **Управление развертываниями** → карандаш → Версия: **Новая**,
тогда URL останется прежним.

## Ограничения

- пак до 5 МБ, до 20 отправок в час на всех, повтор того же пака отклоняется 6 часов;
- картинки только с серверов из списка `IMAGE_SOURCES` (тот же список в `RemoteImages.kt`
  и `tools/validate_community.py`);
- в теле PR символ `@` разбивается, чтобы комментарий не мог никого упомянуть.
