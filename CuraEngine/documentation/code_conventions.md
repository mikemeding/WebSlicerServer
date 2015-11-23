Code Conventions
=======
Note that not all the code convention described here have been fully implemented yet.

Bracketing and indenting
-----
~~~~~~~~~~~~~~~{.cpp}
if (condition) // brackets always on new lines
{ // allways a bracket after an if, for, while, etc.
    // indent always with 4 spaces, never with tabs
}
else // else on new line
{
    // more code
}
~~~~~~~~~~~~~~~

Naming conventions
------
 * variables: lower_case_with_underscores
 * functions: loweCamelCase
 * classes: UpperCamelCase
 * macros: UPPER_CASE_WITH_UNDERSCORES
~~~~~~~~~~~~~~~{.cpp}
#define UPPER_CASE_MACRO 1

class UpperCamelCase
{
private:
    MemberVariableObject with_underscores;
public:
    MemberVariableObject with_underscores;

public:
    UpperCamelCase();
    ~UpperCamelCase();
    
    // start with input variable(s) and end with output variable(s)
    void lowerCamelCaseFunctions(ParamObject& also_with_underscores)
    {
        LocalObject under_scores;
    }
private:
    void putFunctionsAndVariablesInSeperatePublicPrivateBlocks();
};
~~~~~~~~~~~~~~~

Ordering
----
~~~~~~~~~~~~~~~{.cpp}
class Example
{
    // start with input variable(s) and end with output parameter(s) 
    void function1(ParamObject& input_variable, int setting_parameter, ParamObject2& return_parameter)
    {
        function2();
        function3();
    }
    
    // place functions called solely by one other function below it chronologically
    void function2();
    
    void function3();
};
~~~~~~~~~~~~~~~

Pointers vs. References
-----
Use reference wherever you can, pointers wherever you must.

Examples of where pointers can be used are:
- optional values
- variable values
- class members not known at construction yet

Documentation
----
We use [Doxygen](www.doxygen.org/) to generate documentation. Try to keep your documentation in doxygen style.

Here's a small example:
~~~~~~~~~~~~~~~{.cpp}
/ *!
 * Doxygen style comments!
 *
 * \param param1 explanation may refer to another \p param2
 * /
void function(int param1, int param2)
{
    // non-doxygen style comments on implementation details
}

int member; //!< inline doxygen comment on the entry to the left
~~~~~~~~~~~~~~~

Files
--------
For a file Foo.h (UpperCamelCase):
~~~~~~~~~~~~~~~{.cpp}
#ifndef FOO_H
#define FOO_H
// [content]
#endif//FOO_H
~~~~~~~~~~~~~~~


Other
----
~~~~~~~~~~~~~~~{.cpp}
#include <all>
#include <includes>
#include <on>
#include <top>

#include <first_system_includes>

#include <then_library_includes>

#include "finally_local_includes"

enum class EnumExample 
{
    ELEM0 = 0,
    ELEM1 = 1
};
~~~~~~~~~~~~~~~

Illegal syntax
----
~~~~~~~~~~~~~~~{.cpp}
void function()
{
    if (condition)
        single_line_outside_code_block(); // always use braces!
}; // unneccesary semicolon after function definition is not allowed
~~~~~~~~~~~~~~~
