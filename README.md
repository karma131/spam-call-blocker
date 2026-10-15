# QuietCall — lọc cuộc gọi theo thương hiệu

Ứng dụng Android native Java, Android 10 (API 29) trở lên. Có danh mục số từng bị cảnh báo mạo danh, báo cáo cá nhân để chặn số mới, nhập danh mục CSV, quản lý thương hiệu, số luôn cho phép, nhật ký và kiểm tra quy tắc.

## Giao diện 1.2 và luồng chặn theo tên

Giao diện lấy bố cục từ ảnh tham khảo: nền trắng, điểm nhấn xanh, thanh tìm kiếm bo tròn, avatar chữ cái, danh sách nhật ký/thương hiệu, tab gạch chân, nút thêm nổi và thanh điều hướng dưới. Năm mục chính: **Nhật ký, Thương hiệu, Chặn, Báo cáo, Cài đặt**.

Ứng dụng chỉ sàng lọc cuộc gọi bằng `CallScreeningService` và yêu cầu `RoleManager.ROLE_CALL_SCREENING`. Không yêu cầu vai trò điện thoại mặc định, không xây dựng bàn phím quay số, danh bạ hay màn hình nghe/gọi. Android và ứng dụng điện thoại của hệ thống tiếp tục xử lý cuộc gọi. Nhật ký chỉ chứa các sự kiện dịch vụ sàng lọc nhận được, không phải nhật ký cuộc gọi đầy đủ của điện thoại.

**Chặn** quản lý quy tắc cho thương hiệu/số đã biết. **Báo cáo** bổ sung một số làm phiền mới kèm tên thương hiệu và lý do, rồi đưa vào blacklist cá nhân. Báo cáo hiện không được gửi lên máy chủ hay chia sẻ cho người dùng khác.

Màn hình mặc định là **Thương hiệu**, không cần nhập thủ công tất cả số trước khi thử:

1. Nhấn **Chặn theo tên** hoặc nút **+**; nhập `Nova` hoặc `Nova Finance` và chọn **Chặn thương hiệu**.
2. Ứng dụng tra tên/tên viết tắt trong danh mục, áp dụng chặn cho toàn bộ 3 số thuộc Nova Finance. Tab **Đã chặn** hiển thị nhóm đã chọn. Biểu tượng khiên bên phải mỗi dòng cũng cho phép chặn nhanh.
3. Chạm tên thương hiệu để xem các số, chuyển sang tắt chuông, bỏ chặn nhóm, chỉnh sửa hoặc báo cáo số mới.
4. Với số mới, chọn **Báo cáo thêm số của thương hiệu**. Số vào blacklist cá nhân ngay, có nhãn thương hiệu, và hiện trong phần số của nhóm đó. Báo cáo chưa được chia sẻ sang thiết bị khác.
5. Vào **Cài đặt → Kiểm tra một số**, dùng `+1 202 555 0101`, `+1 202 555 0102`, `+1 202 555 0103` để thử việc chặn nhiều số bằng một tên.

Ô tìm kiếm lọc theo tên không dấu, tên viết tắt hoặc số điện thoại. Tên chưa có dữ liệu cần ít nhất một số khi thêm mới. Việc gõ tên chọn **nhóm số đã có trong danh mục**, không tự suy ra chủ số hoặc nhận diện số hoàn toàn mới.

**Nhật ký** hiển thị các sự kiện sàng lọc thực tế. **Cài đặt** chứa số ngoại lệ, kiểm tra quy tắc, nhập CSV và nguồn dữ liệu. Giao diện đã bỏ mô tả phụ dưới tiêu đề và nhãn minh họa trên danh sách thương hiệu.

### Dữ liệu minh họa

| Thương hiệu giả lập | Tên viết tắt | Các số minh họa |
| --- | --- | --- |
| Nova Finance | Nova, NovaFinance | +1 202 555 0101–0103 |
| An Tâm Bảo Hiểm | An Tam | +1 202 555 0111–0113 |
| GreenHome | Green Home | +1 202 555 0121–0123 |
| SunTravel | Sun Travel | +1 202 555 0131–0133 |
| LearnPlus | Learn Plus | +1 202 555 0141–0143 |
| SaleNow | Sale Now | +1 202 555 0151–0153 |

Các tên này là giả lập trong ứng dụng và mặc định **chưa chặn** để người dùng chọn bằng tên. Số dùng mã vùng 202 với dải 555-0100 đến 555-0199 dành cho hư cấu, theo [NANPA](https://nanpa.com/numbering/555-line-numbers). Không phải danh sách cáo buộc doanh nghiệp thực tế gửi spam. Nguồn gốc dữ liệu vẫn có trong **Cài đặt → Nguồn dữ liệu**.

Có thể mô phỏng cuộc gọi đến trên Android Emulator sau khi bật vai trò lọc và chặn Nova Finance:

```powershell
& 'D:\Android\Sdk\platform-tools\adb.exe' -e emu gsm call +12025550101
& 'D:\Android\Sdk\platform-tools\adb.exe' -e emu gsm cancel +12025550101
```

## Giới hạn nhận diện cần hiểu rõ

Tên hiển thị trên màn hình điện thoại **không được đảm bảo chuyển cho `CallScreeningService`**. API chỉ đảm bảo một số trường, trong đó có số gọi. Vì vậy bản này nhận diện thương hiệu bằng danh mục **số → thương hiệu**, không dựa vào đọc màn hình hay một API tên thương hiệu giả định.

- Một thương hiệu có thể có nhiều số; các số đã gắn nhãn đều tuân theo cùng quy tắc.
- **Chỉ nhập tên không đủ để nhận diện một số hoàn toàn mới.** Cần thêm số hoặc nhập/cập nhật danh mục CSV từ nguồn tin cậy. Thương hiệu chưa có số được đánh dấu rõ trong giao diện.
- Ngoài 18 số minh họa, danh mục đóng gói có 6 số được nêu trong các cảnh báo công khai về **mạo danh**. Nhãn không khẳng định số thuộc thương hiệu bị mạo danh hoặc còn hoạt động xấu ở hiện tại. Chưa tích hợp nhà cung cấp caller-ID hoặc máy chủ báo cáo cộng đồng. Tệp trong `examples/` vẫn chỉ là ví dụ định dạng CSV.
- Chặn: yêu cầu Android từ chối cuộc gọi và bỏ thông báo. Cuộc gọi có thể vẫn có trong nhật ký hệ thống. Cách hiển thị cuối cùng tùy hệ điều hành/nhà sản xuất.
- Tắt chuông: yêu cầu im lặng; **không đảm bảo ẩn màn hình cuộc gọi**.
- Không xin quyền đọc danh bạ: Android thường bỏ qua lọc các số trong danh bạ. Cuộc gọi ẩn số và cuộc gọi không phải `tel:` không được đảm bảo chuyển đến dịch vụ; cuộc gọi trong Zalo/Messenger/WhatsApp không thuộc phạm vi này.
- Không xác thực quyền sở hữu số hoặc chống giả mạo caller-ID. Nhãn sai hoặc số được tái cấp có thể dẫn đến nhận diện sai.

Tham chiếu chính thức: [CallScreeningService](https://developer.android.com/reference/android/telecom/CallScreeningService), [Screen calls](https://developer.android.com/develop/connectivity/telecom/dialer-app/screen-calls).

## Mở và build

1. Mở thư mục này trong Android Studio có hỗ trợ AGP 8.9.2.
2. Cài Android SDK Platform 35 và Build Tools 35.0.0 qua SDK Manager; dùng JDK 17 hoặc 21.
3. Đặt SDK trong `local.properties` (Android Studio thường tự tạo), ví dụ `sdk.dir=C:/Users/your-user/AppData/Local/Android/Sdk`.
4. Đồng bộ Gradle, chạy trên điện thoại Android 10+ hoặc build debug APK:

```powershell
.\gradlew.bat :app:assembleDebug :app:lintDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. Gradle Wrapper 8.11.1 đã kèm trong dự án; lần build đầu cần mạng để tải Gradle và dependencies. Đây là APK debug, chưa phải bản ký phát hành lên Play Store.

Trên Windows, nếu gặp `Unable to establish loopback connection`, dùng script sau để đặt thư mục socket tạm riêng cho Java:

```powershell
.\scripts\build-android.ps1
```

Khi đã khởi động đúng một máy ảo Android, có thể build, cài và mở ứng dụng bằng một lệnh:

```powershell
.\scripts\build-android.ps1 -Run
```

Script chỉ cài khi build thành công và tìm thấy APK; chỉ mở ứng dụng sau khi cài thành công. Script đọc đường dẫn SDK từ `local.properties`, dùng cache Gradle tại `.tools/gradle-home`, và khôi phục biến Java sau khi chạy.

## Sử dụng

1. Mở QuietCall, chọn **Kích hoạt trên Android**, chọn QuietCall làm ứng dụng lọc cuộc gọi trong hộp thoại hệ thống.
2. Thêm thương hiệu cần chặn. Nhập nhiều số, cách nhau bằng dấu phẩy hoặc xuống dòng; hoặc nhập CSV trước, rồi bật thương hiệu mong muốn.
3. Chọn **Chặn cuộc gọi** hoặc **Chỉ tắt chuông**. Tạm dừng toàn bộ bằng công tắc bảo vệ.
4. Thêm các số quan trọng vào **Ngoại lệ**. Ngoại lệ được ưu tiên cao nhất.
5. Dùng **Kiểm tra** để xem quyết định theo quy tắc. Đây là mô phỏng quyết định, không kiểm tra được tích hợp cuộc gọi trên thiết bị.
6. Thử gọi từ máy khác với số không có trong danh bạ. Xem quyết định tại **Nhật ký**.

## Danh mục thực tế có sẵn và báo cáo số mới

Phiên bản 1.2 đóng gói `app/src/main/assets/brand-directory.json`, gồm 18 số minh họa và 6 số cảnh báo thực tế dưới đây:

| Nhóm cảnh báo | Các số từng được nêu | Nguồn |
| --- | --- | --- |
| Mạo danh Vietcombank | 02366888766, 02488860469, 02888865154, 1900355561 | [Vietcombank, 03/04/2025](https://www.vietcombank.com.vn/vi-VN/KHCN/Truy-cap-nhanh/Tin-noi-bat/Articles/2025/04/03/canh-bao-rui-ro-gia-mao-nhan-vien-VCB) |
| Mạo danh Điện lực | 0328379693 | [EVN, 31/03/2025](https://www.evn.com.vn/d/vi-VN/news/Nhieu-nguoi-dan-van-mac-bay-lua-dao-cua-cac-doi-tuong-mao-danh-nganh-Dien--60-134-502100) |
| Mạo danh Điện lực | 0917896904 | [Công an tỉnh Gia Lai, vụ việc 08/03/2025](https://congan.gialai.gov.vn/vi/news/antt-trong-tinh/tin-an-ninh-trat-tu-5990.html) |

Nguồn được rà soát ngày 15/10/2026. Đây là danh mục cảnh báo lịch sử nhỏ, không phải danh sách đầy đủ các số spam tại Việt Nam hay xác nhận chủ thuê bao hiện tại. Không đưa các tổng đài hỗ trợ hợp lệ xuất hiện trong bài cảnh báo vào blacklist. Nhóm cảnh báo thực tế mới mặc định bật chặn; các nhóm minh họa mặc định tắt. Có thể tắt nhóm, sửa số hoặc thêm ngoại lệ. Vào **Cài đặt → Nguồn dữ liệu** để đọc nguồn và ngày cảnh báo.

Mỗi phiên bản dữ liệu chỉ được ghép một lần, kể cả khi cập nhật APK trên thiết bị đã cài. Mở lại ứng dụng không tự khôi phục nhóm đã xóa hoặc bật lại nhóm đã tắt. Khi có phiên bản danh mục mới, dữ liệu mới bổ sung vào nhóm cũ và giữ trạng thái nhóm đó; ngoại lệ và báo cáo cá nhân được giữ riêng.

Để thêm số mới: vào **Báo cáo → Báo cáo số spam**, hoặc chọn **Báo cáo & chặn số này** trong chi tiết cuộc gọi. Nhập số, tên thương hiệu/tên tự xưng và lý do. Bỏ trống tên sẽ lưu **Chưa xác định**. Tên viết tắt đã biết sẽ được đổi thành tên thương hiệu chính thức trong danh mục. Báo cáo vào blacklist cá nhân ngay; có hiệu lực khi bật bảo vệ và đã cấp vai trò lọc cuộc gọi. Nó vẫn chặn số đó nếu nhóm thương hiệu đang tắt hoặc chỉ tắt chuông.

Các số báo cáo được lưu riêng, gắn với nhãn thương hiệu do người dùng nhập; không tự thay đổi danh mục cảnh báo có nguồn. Báo cáo lại cùng số (kể cả khác định dạng 0/+84) cập nhật thông tin thay vì tạo bản trùng. Có thể gỡ báo cáo khỏi blacklist. Nếu số vẫn nằm trong nhóm đang chặn, cần sửa nhóm hoặc thêm ngoại lệ để cho phép. Số trong Ngoại lệ luôn được ưu tiên; muốn báo cáo số đang ở Ngoại lệ phải gỡ ngoại lệ trước.

Báo cáo hiện chỉ lưu trên thiết bị này, **chưa được gửi cho cộng đồng, nhà mạng hoặc cơ quan chức năng**. Không yêu cầu Internet hay tự gửi SMS. Gỡ báo cáo khỏi blacklist sẽ xóa nội dung báo cáo cá nhân cho số đó.

Số Việt Nam `0912…`, `+84912…`, `0084912…` được chuẩn hóa về cùng dạng. Số nước ngoài nhập với mã quốc gia. Chỉ khớp toàn bộ số; không dùng đầu số, đuôi số hoặc wildcard.

## Kiến trúc và dữ liệu

- `MainActivity`: giao diện tiếng Việt, yêu cầu vai trò lọc cuộc gọi, quản lý quy tắc, nhập CSV, kiểm tra và nhật ký.
- `ScreeningEngine`: logic Java thuần; thứ tự quyết định: tạm dừng → ngoại lệ → blacklist cá nhân → thương hiệu chặn → thương hiệu tắt chuông → cho phép.
- `SpamReport`: chuẩn hóa, kiểm tra thông tin báo cáo và cập nhật không trùng số.
- `DirectoryCsv`: phân tích CSV và ghép dữ liệu; kiểm tra trước khi lưu.
- `RuleStore`: SharedPreferences riêng của ứng dụng, lưu báo cáo, quy tắc, phiên bản và nguồn danh mục; tối đa 200 sự kiện cuộc gọi mới nhất. Có nút xóa nhật ký và gỡ từng báo cáo, bản sao lưu hệ thống đã tắt.
- `SpamScreeningService`: chỉ xử lý cuộc gọi đến; đọc dữ liệu trên máy và trả lời Android trước khi ghi nhật ký. Không đợi mạng trong thời hạn phản hồi 5 giây. Nếu đọc quy tắc lỗi, cho phép cuộc gọi để tránh chặn nhầm.

Không khai báo quyền Internet, SMS, danh bạ hoặc nhật ký cuộc gọi. Danh mục và nhật ký không được gửi khỏi thiết bị. Gỡ ứng dụng sẽ xóa dữ liệu cục bộ. Hiện chưa có máy chủ hay đồng bộ tự động. Để tự nhận diện các số mới theo thương hiệu ở quy mô thực tế, cần tích hợp nguồn danh mục đáng tin cậy và cập nhật bản lưu cục bộ trước khi có cuộc gọi.
