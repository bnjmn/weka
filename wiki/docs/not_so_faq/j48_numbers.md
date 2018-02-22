```
J48 pruned tree
node-caps = yes
| deg-malig = 1: recurrence-events (1.01/0.4)
| deg-malig = 2: no-recurrence-events (26.2/8.0)
| deg-malig = 3: recurrence-events (30.4/7.4)
node-caps = no: no-recurrence-events (228.39/53.4)
```

The **first** number is the total number of instances (weight of instances) reaching the leaf. The **second** number is the number (weight) of those instances that are misclassified.

If your data has **missing** attribute values then you will end up with **fractional** instances at the leafs. When splitting on an attribute where some of the training instances have missing values, J48 will divide a training instance with a missing value for the split attribute up into fractional parts proportional to the frequencies of the observed non-missing values. This is discussed in the Witten & Frank Data Mining book as well as Ross Quinlan's original publications on C4.5.
