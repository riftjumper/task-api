# Task API + Order API

Branch solusi ini mempertahankan Task API murid dan menambahkan Order API sesuai
[`docs/order-api-pseudocode.md`](docs/order-api-pseudocode.md).

## Menjalankan dengan MySQL

Butuh Java 17, server MySQL yang sedang berjalan, dan akun MySQL yang sudah
dibuat serta memiliki izin untuk membuat database `task_api_db`. Aplikasi
memakai MySQL secara default.

Salin `mysql.properties.example` menjadi `.local/mysql.properties` dari folder
ini:

```bash
mkdir -p .local
cp mysql.properties.example .local/mysql.properties
```

Isi username dan password MySQL di `.local/mysql.properties`, lalu jalankan
`./mvnw spring-boot:run` atau tombol Run di IntelliJ. Folder `.local/`
diabaikan oleh Git agar password tidak ikut ter-commit.

Di IntelliJ, buka `pom.xml` dalam folder ini sebagai project Maven, kemudian
jalankan `TaskApiApplication`. Sebagai alternatif, username dan password dapat
diisi melalui environment variables `DB_USERNAME` dan `DB_PASSWORD`.

Jika nama/host database berbeda, set `DB_URL`. Contoh:

```text
jdbc:mysql://localhost:3306/task_api_db?createDatabaseIfNotExist=true
```

Saat pertama dijalankan, Connector/J membuat database yang belum ada (bila
akun punya izin `CREATE DATABASE`), lalu Flyway menjalankan migration untuk
membuat tabel `tasks`, `products`, dan `orders`. Setelah itu Hibernate
memeriksa kecocokan skema. Migration berikutnya ditambahkan sebagai file
`V3__nama_perubahan.sql` di
`src/main/resources/db/migration/mysql/`; jangan mengubah migration yang sudah
pernah dijalankan.

Untuk database yang sudah ada, Flyway membuat riwayat migration tanpa menghapus
data `tasks`. Aplikasi tidak memasang atau menyalakan server MySQL; server dan
akun MySQL harus disiapkan lebih dulu.

## Mencoba Order API

Tambahkan produk percobaan di MySQL:

```sql
INSERT INTO products (name, price, stock)
VALUES ('Keyboard', 125000.00, 5);
```

Gunakan ID produk yang baru dibuat untuk `POST /orders`:

```json
{"product_id": 1, "quantity": 2}
```

Respons sukses `201` menyimpan order dengan `total_price` dari harga database
dan mengurangi stok dalam satu transaction. `GET /orders/{id}` membaca order
tersebut. Input tidak valid menghasilkan `422`, produk/order tidak ditemukan
`404`, dan stok kurang `409`. Detail contoh request ada di `requests.http`.

## Tes tanpa MySQL

```bash
./mvnw test
```

Tes menggunakan H2 dan migration khusus H2.
