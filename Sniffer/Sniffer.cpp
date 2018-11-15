#include <iostream>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <vector>
#include <sstream>

using namespace std;

int main()
{
    unsigned int a;

    // info that we dont need.
    int ct = 13;
    while(ct--){
        cin >> hex >> a;
    }

    // Source IP
    ostringstream src_ip;
    ct = 4;
    while(ct--){
        cin >> hex >> a;
        src_ip << a;
        if(ct) src_ip << ".";
    }

    cin >> hex >> a;
    // Destination IP
    ostringstream dst_ip;
    ct = 4;
    while(ct--){
        cin >> hex >> a;
        dst_ip << a;
        if(ct) dst_ip << ".";
    }


    ostringstream payload;
    // Source port
    int src_port;
     cin >> hex >> a;
     payload << (char)a;
     src_port = a*16*16;
     cin >> hex >> a;
     payload << (char)a;
     src_port += a;

    // Destination port
    int dst_port;
     cin >> hex >> a;
     payload << (char)a;
     dst_port = a*16*16;
     cin >> hex >> a;
     payload << (char)a;
     dst_port += a;

     // info that we dont need.
     ct = 16;
     while(ct--){
        cin >> hex >> a;
        payload << (char)a;
        if (ct == 8)   cin >> hex >> a;
     }

     //Message itself
     ostringstream msg;
     ct == 0;
     while(a != 10){
        cin >> hex >> a;
        msg << (char)a;

        ct++;
        if(ct%16 == 7 ) cin >> hex >> a;

     }

     cout << endl;
     cout << "Source IP: " << src_ip.str() << endl;
     cout << "Destination IP: " << dst_ip.str() << endl;
     cout << "Source Port: " << src_port << endl;
     cout << "Destination Port: " << dst_port << endl;
     cout << "Payload: " << payload.str() << endl;
     cout << "Message: " << msg.str() << endl;

    return 0;
}