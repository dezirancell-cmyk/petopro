# PetroPro 511 — لایه‌ی سرور برای اپ اندروید (فاز ۱)

## نصب
1. `varzesh510.py` را با `varzesh511.py` جایگزین و مثل همیشه اجرا کنید. مهاجرت دیتابیس خودکار است و فقط `CREATE TABLE IF NOT EXISTS` اجرا می‌شود؛ هیچ داده‌ی موجودی تغییر نمی‌کند.
2. برای Push (اختیاری، بعد از ساخت پروژه‌ی Firebase): در Firebase Console → Project settings → Service accounts → Generate new private key. فایل JSON را روی سرور بگذارید و قبل از اجرا تنظیم کنید:
   `export PETROPRO_FCM_SERVICE_ACCOUNT=/مسیر/firebase-service-account.json`
   بدون این متغیر همه‌چیز کار می‌کند؛ فقط Push ارسال نمی‌شود و اعلان‌ها در صندوق درون‌برنامه‌ای می‌مانند.
3. پکیج `cryptography` برای امضای OAuth لازم است؛ نصب‌کننده آن را به‌عنوان پکیج اختیاری نصب می‌کند.
4. `PETROPRO_DISABLE_SCHEDULER=1` زمان‌بند اعلان‌ها را خاموش می‌کند (پیش‌فرض روشن).

تغییر در کد قبلی: فقط ۸ خط موجود اصلاح شد (شماره‌ی نسخه؛ افزودن مسیر ورود موبایل به معافیت CSRF؛ افزودن `athlete_id` به SELECT وضعیت درخواست برنامه؛ ثبت گزارش آسیب‌دیدگی برای گرفتن شناسه‌ی جدید؛ افزودن `id` به پاسخ JSON ثبت گزارش و ثبت پیام؛ ۲ خط لیست پکیج اختیاری). بقیه‌ی تغییرات فقط «افزودن» است (حدود ۱۲۶۵ خط). رفتار وب بدون تغییر ماند (تست‌های T2/T3/T15).

## جدول‌های جدید
`mobile_tokens`، `mobile_devices`، `request_idempotency`، `app_notifications`، `announcements`. توکن‌ها فقط به‌صورت SHA-256 ذخیره می‌شوند.

## قرارداد API
همه‌ی مسیرها JSON با `ok`. هدر احراز هویت: `Authorization: Bearer <token>` (بدون کوکی و بدون CSRF). اگر هدر Bearer هست ولی نامعتبر است، سرور به کوکی برنمی‌گردد (۴۰۱).

| مسیر | توضیح |
|---|---|
| `GET /api/mobile/config` | عمومی. `api_version`، `push_enabled`، `online_only_features` (AI و ... که آفلاین نیستند) |
| `POST /api/mobile/login` | `{username,password,device_id,device_name}` → `token`, `expires_at` (۹۰ روز), `user` |
| `POST /api/mobile/logout` | توکن را باطل و FCM همان دستگاه را جدا می‌کند |
| `GET /api/me` و همه‌ی APIهای قبلی | با Bearer کار می‌کنند (همان سطح دسترسی نقش‌ها) |
| `POST /api/mobile/sets/log` | ثبت ست با **وضعیت صریح**: `{item_id,set_number,date,done,weight_kg,reps_done,rpe,note,client_ts, program_id,day_index,exercise_id,order_index}` |
| `POST /api/mobile/checkin` | چک‌این با `date` و `client_ts` (حداکثر ۱۴ روز قبل) |
| `GET /api/mobile/sync/manifest[?athlete_id=]` | لیست برنامه‌ها با `hash` محتوایی، `media.signature`، `unread_notifications` |
| `GET /api/mobile/sync/media-manifest[?athlete_id=&active_only=1]` | هر فایل: `url, kind(gif/video/image), local, size, sha256, exercise_ids` |
| `POST /api/mobile/devices` / `DELETE /api/mobile/devices/<id>` | ثبت `{device_id,fcm_token,app_version,notifications_enabled}` |
| `GET /api/mobile/notifications[?after_id=&limit=&kind=]` | صندوق اعلان‌ها + `unread_count` |
| `POST /api/mobile/notifications/read` | `{ids:[..]}` یا `{all:true}` |
| `POST/GET /api/announcements`, `DELETE /api/announcements/<id>` | اطلاعیه (مدیر/مربی). `target`: مدیر `all_athletes|all_coaches|everyone|users`، مربی `my_athletes|users`؛ `scheduled_at` به وقت تهران (`2026-11-21T09:30`) |
| `GET/PUT /api/mobile/smart-config`, `POST .../run-now` | تنظیم و اجرای دستی اعلان‌های هوشمند (فقط مدیر) |

## قواعد کلاینت (Outbox)
- هر عملیات نوشتنی در Outbox یک `X-Idempotency-Key` ثابت (UUID یا ۸–۱۰۰ نویسه‌ی `A-Za-z0-9_-`) می‌گیرد و با **همان کلید** دوباره ارسال می‌شود. پاسخ تکراری با هدر `Idempotent-Replay: true` برمی‌گردد. کلید ۱۴ روز نگهداری می‌شود.
- `device_id` را یک‌بار بسازید و هم در login و هم در `/devices` همان را بفرستید.
- مدیریت پاسخ‌ها: `2xx` حذف از صف؛ `400/404/409(conflict)/422` قطعی است → از صف خارج و به کاربر نشان داده شود؛ `409 request_in_progress` و `429` و `5xx` و خطای شبکه → دوباره با همان کلید؛ `401` → ورود مجدد و حفظ صف؛ `403 panel_sub_expired` → عملیات معلق بماند (اشتراک تمام شده).
- تعارض ست: اگر مربی در وب برنامه را ذخیره کند، شناسه‌ی حرکت‌ها عوض می‌شود. سرور با `program_id+day_index+exercise_id(+order_index)` به حرکت جدید نگاشت می‌کند (`remapped_item_id`)؛ اگر حرکت حذف شده باشد `409 conflict=item_removed` برمی‌گردد و ثبت بی‌صدا گم نمی‌شود.
- تعارض ست موجود: سرور برنده است (`unchanged=true` + `server_state`). تعارض چک‌این: جدیدترین `client_ts/logged_at` برنده است (`conflict=server_newer`).
- GIFها از همان `/static/...` با Range/ETag دانلود می‌شوند (دانلود ناقص قابل ادامه). فایل را با `sha256` تأیید کنید. آدرس‌هایی با `local:false` مستقیم از منبع خارجی‌اند و در شبکه‌های فیلتر ممکن است باز نشوند؛ بهتر است مدیر کتابخانه را «همگام‌سازی» کند تا محلی شوند.

## اعلان‌ها
نوع‌ها و `data.route` برای باز کردن صفحه: `injury_new/injury_reply/injury_message/injury_status` → `injury/<id>`؛ `program_created/program_updated` → `program/<id>`؛ `program_request`؛ `announcement` → `announcements`؛ `workout_reminder`، `checkin_reminder`، `injury_unanswered`، `sub_expiry`. کانال‌های اندروید: `injury، program، announcements، reminders، general`. Push هم `notification` و هم `data` دارد (`nid, kind, route, ...`).
قواعد هوشمند (قابل تنظیم/خاموش): یادآوری تمرین (۲ تا ۷ روز بعد از آخرین تمرین، ساعت ۱۷)، یادآوری چک‌این (فقط برای کسانی که در ۱۴ روز اخیر چک‌این داشته‌اند، ساعت ۹)، گزارش آسیب‌دیدگیِ بی‌پاسخ >۲۴ ساعت برای مربی، نزدیک شدن پایان اشتراک پنل (≤۳ روز). هر قاعده روزی یک‌بار و با کلید dedupe ثبت می‌شود. چون مدل برنامه «روز هفته» ندارد، «روز تمرین» را نمی‌توان دقیق دانست؛ قاعده بر اساس فاصله از آخرین تمرین است.

## محدودیت‌های شناخته‌شده
- فاصله‌ی استراحت (`actual_rest_seconds`) برای ست‌های آفلاین دقیق نیست (روی ساعت سرور محاسبه می‌شود؛ `logged_at` بعداً به زمان واقعی اصلاح می‌شود).
- تغییر رمز و «خروج اجباری همه» توکن‌های موبایل را هم باطل می‌کنند.
- قفل زمان‌بند (`pg_try_advisory_xact_lock`) مخصوص PostgreSQL است و با چند پردازه هم اعلان تکراری نمی‌سازد (dedupe هم دارد).
- زمان `created_at` گزارش‌های آسیب‌دیدگی بر اساس منطقه‌ی زمانی خود دیتابیس است؛ قانون «۲۴ ساعت بی‌پاسخ» ممکن است حداکثر چند ساعت جابه‌جا شود.
- نکته‌ی امنیتی: رمز PostgreSQL به‌صورت ثابت در `PG_DSN_FOR_APP` داخل خود فایل نصب‌کننده است. بهتر است فقط از متغیر `PETROPRO_PG_DSN` استفاده کنید و رمز را از فایل حذف کنید.

## گزارش تست (آنچه واقعاً اجرا شد)
`phase1/test_mobile_layer.py`: ۹۸ بررسی، همه موفق. کد واقعیِ `app.py` (استخراج‌شده از `varzesh511.py`) با Flask test client اجرا شد، ولی روی **SQLite** (محیط من PostgreSQL ندارد) و با اسکیمای حداقلی که خودم نوشتم. Firebase با یک **سرور ساختگی محلی** تست شد که امضای RS256 JWT را واقعاً راستی‌آزمایی می‌کند. برای اجرا: `app.py` را از `APP_PY` بیرون بکشید، `db_compat_SQLITE_TEST_SHIM.py` را کنارش با نام `db_compat.py` بگذارید و `python test_mobile_layer.py` را اجرا کنید (این شیم فقط برای تست است، هرگز روی سرور اصلی نگذارید).

**اجرا نشد / تأیید نشده:** PostgreSQL واقعی، Firebase/OAuth واقعی گوگل، بار و رقابت واقعیِ هم‌زمان (فقط حالت pending/stale شبیه‌سازی شد)، سرور و دیتابیس واقعی شما، و کل اپ اندروید (فاز ۲). قبل از استفاده، یک‌بار روی کپی دیتابیس خودتان اجرا کنید و ۳ سناریو را دستی بزنید: ورود موبایل، ثبت ست با کلید تکراری، و ارسال اطلاعیه.
