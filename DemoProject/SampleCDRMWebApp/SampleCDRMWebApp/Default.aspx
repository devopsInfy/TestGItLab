<%@ Page Title="Home Page" Language="C#" MasterPageFile="~/Site.Master" AutoEventWireup="true" CodeBehind="Default.aspx.cs" Inherits="SampleCDRMWebApp._Default" %>

<asp:Content runat="server" ID="FeaturedContent" ContentPlaceHolderID="FeaturedContent">
    <section class="featured">
        <div class="content-wrapper">
            <hgroup class="title">
               
                <h2>CDRM Web Application Prod Test-Release</h2>
            </hgroup>
            
        </div>
    </section>
</asp:Content>
<asp:Content runat="server" ID="BodyContent" ContentPlaceHolderID="MainContent">
    <h3>This is Sample Application</h3>
    <table>
        <tr>
            <td class="auto-style1"> First Name</td>
            <td>
                <asp:TextBox ID="TextBox1" runat="server"></asp:TextBox>
            </td>
        </tr>
        <tr>
  <td> Last Name</td>
            <td>
                <asp:TextBox ID="TextBox2" runat="server"></asp:TextBox>
            </td>
        </tr>
        <tr>
              <td> Full Name</td>
            <td>
                <asp:Label ID="Label1" runat="server" Text="Label" Visible="False"></asp:Label>
              </td>
        </tr>
        <tr>
            <td rowspan="2" colspan="2" >

                <asp:Button ID="Save" runat="server"  Text="Save" OnClick="Save_Click"  />

            </td>
        </tr>
    </table>
</asp:Content>
