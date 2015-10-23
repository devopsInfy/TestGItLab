using System;
using SampleCDRMWebApp;
using Microsoft.VisualStudio.TestTools.UnitTesting;

namespace UnitTestProject1
{
    [TestClass]
    public class UnitTest1
    {
        [TestMethod]
        public void SampleStringConcatenateMethod()
        {
            string FirstName = "John";
            string LastName = "Walker";
            string Name = "JohnWalker";
            Assert.AreEqual(Name, FirstName + LastName);
        }
        [TestMethod]
        public void SampleEqualMethod()
        {
           
            string Name = "JohnWalker";
            Assert.AreEqual(Name,"JohnWalker");
        }

        [TestMethod]
        public void SampleMultiply()
        {
            int number1=8;
            int number2 = 2;
            int result=16;
            Assert.AreEqual(result, number1 * number2);
        }

        [TestMethod]
        public void SampleSubtract()
        {
            int number1 = 8;
            int number2 = 2;
            int result = 6;
            Assert.AreEqual(result, number1 - number2);
        }

        [TestMethod]
        public void SampleDivision()
        {
            int number1 = 8;
            int number2 = 2;
            int result = 4;
            Assert.AreEqual(result, number1 /number2);
        }
    }


}
