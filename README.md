# buffer-placement-benchmark

Воспроизводимый JMH-замер из семинара «JVM: память и GC» (неделя 4): heap
(`ByteBuffer.allocate`) против direct (`ByteBuffer.allocateDirect`) буфера по
трём операциям — аллокация, доступ из Java, передача через `FileChannel`.

Заменяет учебный скрипт `BufferPlacementDemo` корректной методологией:
прогрев JIT, форки JVM, статистика по итерациям, JSON-отчёты.

## Бенчмарки

| Класс | Что меряет | Операция |
|---|---|---|
| `AllocationBenchmark` | Стоимость создания буфера, без и с first-touch страниц | 1 аллокация |
| `JavaAccessBenchmark` | Доступ из Java: `fill+sum` через `LongBuffer`-view (цикл из демо), чтение через view, абсолютные `getLong`/`putLong`, базовая линия `long[]` | 1 проход по буферу |
| `FileIoBenchmark` | Запись/чтение 64 МиБ через `FileChannel` (файл в page cache — меряется путь в JVM, не диск) | 64 МиБ передачи |

Параметр `bufferBytes` по умолчанию 4 МиБ — как в демо (`bufferMiB=4`).

## Запуск локально

```bash
./gradlew jmhJar

# быстрый прогон (~5 минут)
java -jar build/libs/*-jmh.jar -f 1 -wi 3 -i 5

# полный прогон (параметры из аннотаций: 3 форка, 5x1s прогрев, 10x1s замер)
java -jar build/libs/*-jmh.jar

# отдельный бенчмарк, другой размер буфера
java -jar build/libs/*-jmh.jar "JavaAccessBenchmark" -p bufferBytes=1048576
```

Результаты печатаются в `ns/op`. Пересчёт в МиБ/с для сравнения с демо:

- `fillAndSum*` / варианты fill: `2 * bufferBytes / score`
- `sum*`: `bufferBytes / score`
- `FileIoBenchmark.*`: `64 МиБ / score`

Например `fillAndSumHeapView = 500 000 ns/op` при 4 МиБ буфере —
это `2 * 4194304 B / 0.0005 s ≈ 16 000 МиБ/с`.

## Запуск на GitHub Actions

- По push в `main` — быстрый прогон на матрице JDK 21/25 (Temurin,
  `ubuntu-latest`), JSON с результатами сохраняется артефактом.
- `workflow_dispatch` с `full=true` — полный прогон по параметрам
  из аннотаций.

Артефакты `jmh-results-jdk*` содержат стандартный JMH JSON
(скор + доверительный интервал 99.9%).

## Как читать результат

Ожидаемая картина (и то, что показывало демо):

- **Аллокация**: direct дороже (malloc + обнуление + учёт в `Cleaner`),
  особенно с first-touch. Отношение > 1 нормально.
- **Доступ из Java**: heap не медленнее direct, обычно быстрее — доступ к
  `byte[]` JIT устраняет bounds-checks и векторизует лучше, чем `Unsafe`-
  доступ по сырому адресу. Отношение direct/heap < 1 — не аномалия.
- **FileChannel I/O**: direct быстрее, потому что heap-буфер JVM копирует
  во временный direct-буфер перед нативным вызовом. Это и есть сценарий,
  ради которого direct-буферы существуют.

Вывод демо подтверждается: для обычной обработки данных в Java — heap;
direct — на границе нативного I/O, с переиспользованием буфера.
