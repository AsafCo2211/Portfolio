#ifndef GROWER_H
#define GROWER_H

#include <string>
#include <vector>
class Gardener;
class FlowersBouquet;

class Grower {
private:
    std::string name;
    Gardener* gardener;

public:
    Grower(std::string name, Gardener* gardener);
    FlowersBouquet* prepareOrder(std::vector<std::string>& flowers);
    std::string getName()  ;
};

#endif

