# Packages
PACKAGES is a tool to maintain a repository of packages of electronic components.
It is developed with the goal that various applications use one and the same
repository as a basis for their handling of packages. The repository is in a
well-documented JSON format, easy to parse in a variety of programming languages.

Many packages for electronic components exist, but it is not as straightforward
as having a long list. On occasion a package name refers to a group of packages,
and package name on its own is not conclusive about all dimensions of the package.
Essentially the same package may also be known under multiple names --this is in
part the result of various standardization bodies that each on their own
standardize packages, without regard to the other organizations doing the same.

EDA suites (Electronic Design Application) focus on a flattened view of the
package: the footprint. For mechanical design, the height of the package is
important too, and pick-&-place machines often need to know the shape of the
terminals (for example, gull-wing versus lug-lead) to recognize and centre the
component on the nozzle. That is to say: these various applications all use
package data, but they don’t all need the same data. PACKAGES groups the data
in a structured way, and makes it easily searchable (using both keyword search
and parametric search).

![main user interface of the application](https://github.com/compuphase/Packages/blob/main/doc/mainview.png)

The PACKAGES program comes with an example repository of the standard SMT packages
(0805, TSSOP, SOT23, QFP and QFN, etc.), plus documentation. An important goal
of this tool is to enable multiple applications to use/exchange a single set of
package data. Which is why the file lay-out is documented in detail (and which is
why a widely supported file format was chosen to store the repository in).
