# Pseudocode PR Minggu 1: Order API

Dokumen ini adalah rancangan perilaku untuk PR Order API minggu pertama, bukan
implementasi yang sudah ada di repo ini. Nama class/method boleh berbeda di
Laravel atau Spring Boot; yang penting alur dan tanggung jawabnya sama.

## Data dan endpoint

- `products`: `id`, `name`, `price`, `stock`
- `orders`: `id`, `product_id`, `quantity`, `total_price`, `status`
- `POST /orders` dengan body `{ "product_id": 1, "quantity": 2 }`
- `GET /orders/{id}` untuk membaca order yang sudah tersimpan

Harga dan stok selalu diambil dari database. Jangan menerima `total_price` dari
request; nilai itu dihitung dari harga produk saat order dibuat.

## POST /orders

```text
ROUTE POST /orders -> OrderController.create(request)

CONTROLLER:
    validasi bentuk input:
        product_id wajib ada dan berupa ID yang valid
        quantity wajib ada, berupa bilangan bulat, dan > 0
    jika input tidak valid -> response 422 dengan detail field yang salah

    panggil OrderService.create(product_id, quantity)
    jika berhasil -> response 201 dengan data order yang dibuat
    jika produk tidak ditemukan -> response 404
    jika stok tidak cukup -> response 409
    jika gangguan database/sistem -> log error, response 500

SERVICE create(product_id, quantity):
    BEGIN TRANSACTION
    try:
        product = cari product_id di database, kunci baris selama transaksi
        jika product tidak ada -> error ProductNotFound

        jika product.stock < quantity -> error InsufficientStock

        total = product.price * quantity
        order = simpan order dengan:
            product_id = product.id
            quantity = quantity
            total_price = total
            status = "CREATED"

        product.stock = product.stock - quantity
        simpan perubahan stok product

        COMMIT
        return order
    catch error:
        ROLLBACK
        lempar error ke controller/handler untuk diubah menjadi response
```

Validasi bentuk request dilakukan sebelum transaction. Pencarian produk dan
pengecekan stok dilakukan di dalam transaction, sehingga keduanya memakai
kondisi database yang sama dengan proses menyimpan order. Penguncian baris
produk mencegah dua request bersamaan menghabiskan stok yang sama. Pada Laravel
ini dapat berupa `lockForUpdate()`; konsepnya tetap sama di framework lain.

Jika penyimpanan order berhasil tetapi penyimpanan stok gagal, `ROLLBACK`
membatalkan keduanya. Demikian juga jika produk tidak ada atau stok kurang:
tidak ada order baru dan stok tidak berubah. Error yang diperkirakan (404/409)
dibedakan dari gangguan sistem (500); log detail internal tidak dikirim ke klien.

## GET /orders/{id}

```text
ROUTE GET /orders/{id} -> OrderController.show(id)

CONTROLLER:
    validasi format id
    jika tidak valid -> response 400

    order = cari order berdasarkan id
    jika tidak ada -> response 404
    jika ada -> response 200 dengan data order
```

## Kasus untuk review dan tes

| Kasus | Hasil yang diharapkan |
| --- | --- |
| `product_id=1`, `quantity=2`, stok mencukupi | 201; satu order tersimpan; stok berkurang 2; total dari harga database |
| `quantity` kosong, 0, negatif, atau bukan bilangan bulat | 422; tidak ada perubahan database |
| `product_id` kosong atau tidak valid | 422; tidak ada perubahan database |
| Produk tidak ditemukan | 404; tidak ada order baru |
| Stok kurang | 409; tidak ada order baru; stok tetap |
| Jumlah yang diminta tepat sama dengan stok | 201; stok menjadi 0 |
| Penyimpanan stok gagal setelah order dibuat | 500; transaction rollback; order dan stok tetap seperti semula |
| `GET /orders/{id}` untuk ID yang tidak ada | 404 |

Saat review, minta murid menunjukkan di mana setiap langkah terjadi pada
kodenya, terutama sumber harga, batas transaction, dan siapa yang mengubah
error menjadi response HTTP.
