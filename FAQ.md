[Installation](Installation.md) - [Screenshots](Screenshots.md) - [FAQ](FAQ.md) - [About](About.md)

# Troubleshooting #

**Q: CSS-X-Fire seems to not pick up Firebug changes?**

A: CSS-X-Fire starts a web server on port 6776 when the IDE is started. If this port is already taken by another application CSS-X-Fire will not be able to listen to incoming changes from Firebug.

**Q: I changed a CSS property in Firebug but CSS-X-Fire didn't pick it up even though I know it is running. Why?**

A: The selector can not be found in the project for some reason. The selector is looked up using IntelliJ's internal search mechanism. Copy the selector name from Firebug and paste it into the IDE's "Find in path" and choose to search in entire project. If it can be found here please file a bug for CSS-X-Fire.

**Q: Why is CSS-X-Fire plugin grayed out in Firefox add-ons list?**

A: The plugin has set constraints on which version of Firefox and Firebug it will work with. If these are not met the plugin will be disabled. See the [Installation](Installation.md) page for requirements.

**Q: Why does not CSS-X-Fire work as expected after an update?**

A: With every new version of the IDE plugin there is also a new version of the browser plugin. The corresponding browser plugin is always included in the IDE plugin. See step 2 in the [Installation](Installation.md) details.

**Q: After installation/upgrade I was offered to update the browser plugin. For some reason I did not do it. How can I get the offer again?**

A: Simply press the "Help" button in the CSS-X-Fire toolwindow. See screenshot:
> <a href='http://css-x-fire.googlecode.com/svn/images/install_4.png'><img src='http://css-x-fire.googlecode.com/svn/images/install_4.png' width='120' height='80' /></a>

**Q: Can I have the browser extension on one computer and have the IDE on another?**

A: Yes (with the Firefox extension). Open about:config and type cssxfire in the filter box. Now you should find the host setting which you may change to your desired host.

# Collecting logs #

If you have a problem with the plugin or think that you have found a bug, CSS-X-Fire has a debugging mechanism which can be activated by following the steps below. Finding the cause of the problem is a lot easier with this debug information.

  1. Close the IDE
  1. Open file <IDE installation>/bin/log.xml with a text editor
  1. Scroll to the bottom and add the following lines **before** the `<root>` tag:
```
  <category name="com.github.cssxfire">
       <priority value="DEBUG"/>
  </category>
```
  1. Start the IDE and reproduce the problem
  1. Search for the log file **idea.log**, usually in a subfolder of your home directory