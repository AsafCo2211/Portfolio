// main.cpp

#include <iostream>
#include <vector>
#include "FlowersBouquet.h"
#include "Person.h"
#include "Florist.h"
#include "Wholesaler.h"
#include "Grower.h"
#include "Gardener.h"
#include "FlowerArranger.h"
#include "DeliveryPerson.h"

int main() {
    Gardener* gardener = new Gardener("Garett");
    Grower* grower = new Grower("Gray", gardener);
    Wholesaler* wholesaler = new Wholesaler("Watson", grower);
    FlowerArranger* arranger = new FlowerArranger("Flora");
    DeliveryPerson* delivery = new DeliveryPerson("Dylan");
    Florist* florist = new Florist("Fred",
        wholesaler,
        arranger,
        delivery);

    Person chris("Chris");
    Person robin("Robin");

    std::vector<std::string> flowers = {
        "Roses",
        "Violets",
        "Gladiolus"
    };
    chris.orderFlowers(florist, &robin, flowers);

    delete florist;
    delete delivery;
    delete arranger;
    delete wholesaler;
    delete grower;
    delete gardener;

    return 0;
}
