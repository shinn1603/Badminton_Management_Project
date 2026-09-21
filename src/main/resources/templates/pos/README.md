# Huong dan phan he Nhan vien quay (Le tan & Ban hang POS) (Danh cho Thanh vien 2)

Thu muc nay danh rieng cho Thanh vien 2 xay dung cac man hinh theo Chuong 5:
- MH-NVQ01: Dang nhap noi bo theo chi nhanh
- MH-NVQ02: So do ban co san trong ngay
- MH-NVQ03: Tiep nhan khach & Dat san tai quay
- MH-NVQ04: Check-in & Check-out san
- MH-NVQ05: Quay dich vu: Cho thue dung cu & Ban do uong
- MH-NVQ06: Lap hoa don & In hoa don thanh toan
- MH-NVQ07: Tra cuu thong tin dat san & Khach hang

## 1. Muc tieu chuc nang va doi tuong chinh

Phan he Nhan vien quay phuc vu cac cong viec lien quan den le tan, ban hang POS va quan ly su dung san tai chi nhanh:
- Dang nhap theo chi nhanh va pham vi lam viec cua nhan vien quầy
- Quan sat trang thai san theo thoi gian thuc, phan biet san trong, da dat online, dang choi va bao tri
- Tiep nhan khach online den gio choi va tao don dat san nhanh cho khach vang lai
- Ghi nhan gio check-in va check-out de tinh tien theo thoi gian thuc te
- Ban them dung cu va do uong; he thong tu dong tu tru kho
- Lap hoa don, tinh thanh tien, ap dung chiet khau, tien coc da tra va in hoa don
- Tra cuu nhanh thong tin dat san, khach hang, ma don, so dien thoai de xu ly khieu nai

## 2. Giao dien can phai ho tro

1. Gan the CSS dung chung:
   `<link rel="stylesheet" href="/css/main.css">`
2. Su dung mau giao dien bo sung tu trang luoi san da co: `src/main/resources/templates/pos/grid.html`
3. Lam theo phong cach modal popup, form control, sidebar va dashboard manage cua he thong
4. Tich hop dinh dang tien te theo chuan Viet Nam: `xx.xxx đ`
5. Phai co trang thai san ro rang:
   - xanh: Trong
   - vang: Da dat online
   - do: Dang choi
   - xam: Bao tri

## 3. De xuat bo cuc giao dien theo moi man hinh

### MH-NVQ01 - Dang nhap noi bo theo chi nhanh
- Form dang nhap voi: ma nhan vien / email, mat khau, chi nhanh
- Hien thi thong tin chi nhanh phu tra, vi tri, vai tro nhan vien
- Sau khi dang nhap, quay ve trang san theo ngay hien tai
- Du lieu chi hien thi theo chi nhanh duoc gan cho nhan vien

### MH-NVQ02 - So do ban co san trong ngay
- Bang luoi san theo thoi gian, hang la san, cot la khung gio
- Muc mau theo trang thai san
- Click vao o san de xem chi tiet don dat hoac tao dat san nhanh
- Header gom ngay, chi nhanh va legend trang thai

### MH-NVQ03 - Tiep nhan khach & Dat san tai quay
- Modal tao don dat san nhanh: khach vang lai / khach thanh vien
- Chon san, gio bat dau, gio ket thuc, so dien thoai, ten khach
- Tinh tien theo gio va khoa cọc 30%
- Xac nhan va giu cho 10 phut theo quy dinh

### MH-NVQ04 - Check-in & Check-out san
- Nguoi quay xac nhan nhan san khi khach den
- Ghi nhan gio thuc te check-in/check-out
- He thong tinh lai tong tien theo thoi gian su dung thuc te
- Cap nhat trang thai san tu trong sang dang choi va thanh toan

### MH-NVQ05 - Quay dich vu: Cho thue dung cu & Ban do uong
- Danh sach vat dung: vot, cau, nuoc suối, nước giải khát...
- Chon san pham, so luong, ap dung gia ban theo kho
- Tru kho ngay lap tuc va cap nhat so du con lai
- Hien thi tong phu phi va don hang ben phai giao dien

### MH-NVQ06 - Lap hoa don & In hoa don thanh toan
- Tong hop: tien gio thuc te + dung cu + do uong - chiet khau - tien coc da tra
- Hien thi thong tin khach hang, san, phu phi va thong tin thanh toan
- Co chuc nang in hoa don tien mat / chuyen khoan / QR
- Ngan can khong thanh toan khi du lieu chua hoan chinh

### MH-NVQ07 - Tra cuu thong tin dat san & Khach hang
- Tim kiem nhanh theo so dien thoai hoac ma don dat san
- Hien thi lich su dat san, thong tin khach, trang thai thanh toan
- Cho phep xem chi tiet, dat lai, sua thong tin hoac chuyen sang tao hoa don

## 4. Hướng dẫn thực hiện trong project

- Danh muc giao dien chinh: `src/main/resources/templates/pos/`
- Trang demo lưới đặt sân đã có sẵn: `src/main/resources/templates/pos/grid.html`
- File CSS dùng chung: `src/main/resources/static/css/main.css`
- Controller Spring Boot liên quan: `src/main/java/vn/yain/controller/DashboardController.java`

## 5. Lưu ý thiết kế và bắt buộc

- Không làm rời rạc hoặc phân tán theo tình huống thực tế, phải giữ giao diện uy tín và dễ thao tác
- Quan trọng nhất là tương thích với quy trình: nhận khách -> check-in -> cho thuê/bán hàng -> thanh toán -> in hóa đơn
- Các màn hình nên hỗ trợ thao tác ngay trên cùng một dashboard để làm việc nhanh cùng lúc
- Nếu cần làm thêm modal cho thao tác nhanh, nên dùng bố cục tương tự `createBookingModal` và `detailBookingModal` đã có trong `grid.html`

## 6. Tham khảo mẫu có sẵn

- Trang lưới đặt sân hiện thị trạng thái theo khung giờ: `src/main/resources/templates/pos/grid.html`
- Cấu trúc modal và form control tương tự: `src/main/resources/templates/admin/users.html`
- Tài liệu hệ thống chi nhánh và kho vật tư: `src/main/resources/templates/manager/README.md`

## 7. Kết luận

Phan he Nhan vien quay la tinh nang quan trong nhat cho hoat dong ngay cua chi nhanh: ke ca qua trinh le tan, dat san, check-in, cho thue dung cu, thanh toan va in hoa don. Do vay, giao dien nen tap trung vao toc do thao tac, ro rang trang thai san va minh bach thong tin khach hang, hoa don va kho hang.
