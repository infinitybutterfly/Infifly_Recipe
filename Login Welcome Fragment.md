1. ~~Login Welcome Fragment~~

   1. ~~Enter Welcome Layout~~
2. ~~Login Create Account Fragment~~

   1. ~~Enter Number Layout~~
   2. ~~Enter OTP Layout~~
   3. ~~Select Fav Food Layout~~
   4. ~~Select Allergic Food Layout~~
   5. ~~All Set/Done Layout~~
3. ~~Home Fragment~~
4. ~~Popular Recipe Layout~~
5. ~~For You Recipe Layout~~
6. ~~Discover Fragment~~
7. ~~Add Recipe Fragment~~
8. ~~My Recipe Fragment~~





1. ~~Search Layout~~
2. ~~Recipe Details Layout~~
3. ~~discover fragment - recipe layout~~
4. ~~uploaded recipes layout~~





profile fields



profile pic

chef's name

country

number(get saved at the login time through cache sharedpreference and get it to make it visible in the profile segment but the number should be ineditable)

userid

bio









in api





~~1 request-otp~~

~~2 verify-otp~~



after authorization



~~3 recipes add~~

~~4 recipes search~~

~~5 help~~

~~6 update profile~~



~~edit\_profile\_back button~~

~~recipe for you sort~~

~~my recipe sort~~

~~popular recipe dots color changing or make it look more appealing~~

~~user profile\_details image not showing~~

~~in details fragment if ingredients or quantity is empty do not show them~~

~~add hint at the bottom of add recipe fragment~~

~~email entering doesnot go up~~





glitches



1. ~~After creating account once, when the app is opened again the app start from the start~~
2. ~~Add Recipe fragment always shows add recipe, even if the profile is newly created and not even a single profile data has been added.~~
3. ~~But now until I open edit profile fragment and click on save profile. even if the profile already created it will still show, fill accounts details.~~
4. ~~in profile fragment the user image profile always shows the regular app fitted ic\_gallary image even if their is an image uploaded by the user~~
5. transition were not clear
6. ~~help center fragment is not working, also in help center fragment bottom nav bar is always showing, hide it.~~
7. ~~My Recipe Fragment recyclerview layout and making, backend logic~~
8. ~~discover fragment also shows nothing~~

   1. ~~discover fragment item click layout~~
9. ~~details fragment not showing anything on the user uploaded recipes~~
10. ~~in search showing fragment their is also bottom margin than the regular~~
11. when anything is searched save the last 5 searches
12. ~~on pressing logout the app does not go to the create account fragment, but delete all the cache and remove the logged in account~~
13. ~~when entering email the input box layout the text went to the next line~~
14. also the button is below the keyboard
15. ~~on keyboard the enter button is the next line button. change it to enter button~~
16. ~~I have to double tap on the submit otp button to submit the otp and go to the next page.~~
17. ~~Same for Get Started button~~
18. ~~after add recipe is added it does not return to anywhere, It should return to My Recipe Fragment~~





~~loader for the screen when the data is loading~~

~~display id, profile image camera click~~

~~recipe upload image camera click~~



~~on click save button profile going to the displaylayout=0 is fine, but then I also want to refresh the fragment~~

~~and when sometimes the after capturing the image the fragment gets reset, it gets restored but, the displaychild number remain =0, so also update that layout displaychild, not only show the edit\_profile\_layout, as it I know this because after camera image capturing the edit\_profile\_layout refresh is enabled, I have set it disabled always by default, also now the username check function is not working properly since last change~~

&#x20;~~on the edit\_profile\_layout username check is red even when the username == original username, and when I refresh the profile, then open the edit\_profile\_layout then it shows normally,~~

now two glitches,
~~1 country name only shows the selected country name not the full array list~~

~~2 when an image is captured using camera, and the fragment gets reset only the image in the edit layout gets reset to its original image, not the latest captured image~~



~~subscription~~





