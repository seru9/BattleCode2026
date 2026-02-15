# Battlecode 2026 Scaffold - Java
### Commit 10.01. 0: 40
Rozdzieliłem to na pare plików, żeby czytelniej kod się czytało, generalnie jak widać są zaimplementowane tylko podstawowe funkcje typi spawnRat, Collector.moveTo etc.. Co sprawia, że kod jest troche głupi i implementuje podstawowe czynności. Trzeba jakieś edgecase'y typu rozpatrywać w nowych wersjach (to-do) 

### proTips
### Komunikacja
Problem jest taki, że to jest w zasadzie jedna tablica - czy to umownie wpisujemy info swoje. (np. array[0], array[1] - pozycja x,y rat kinga (problem ze ratkingow moze byc wiele)) - Jeżeli jest tak jak myśle?
To można opracować z góry liczbe informacji jakie poszczególny ratKing dawać np pozycja x, y (2ideksy), enemy rat_king (2indeksy), info bool is_attacked (1). I ideksować index_informacji *id_rat_kinga
Komunikacja jest oparta o bytecode'y, które robi każda z funkcji na dole jest lista ile bytecodów wykorzustuje poszczególna funkcja.
https://github.com/battlecode/battlecode26/blob/master/engine/src/main/battlecode/instrumenter/bytecode/resources/MethodCosts.txt

### TO-DO
1) Gra na default-small map sprawia, że rat-King siedzi w miejscu zaś jeden enemy babyRat powoli zabija.
   1.1) Dodaj ucieczkę Ratkinga oraz do sharedArray daj informacje o byciu pod atakiem.
   1.2) BabyRaty nie zabijają enemy babyRaya (który zabija króla) to sie musi zmienić.


Look at `gradle.properties` for project-wide configuration.

If you are having any problems with the default client, please report to teh devs and
feel free to set the `compatibilityClient` configuration to `true` to download a different version of the client.
