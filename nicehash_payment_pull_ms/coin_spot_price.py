#!/usr/bin/python
import os, socket, urllib.request, json, sys

def getCoinSpotPrice(time, coin):
    target_url = "https://min-api.cryptocompare.com/data/pricehistorical?fsym=" + coin + '&tsyms=USD&ts=' + time;
    try:
        with urllib.request.urlopen(target_url) as url:
            if not url:
                return
            data = json.loads(url.read().decode())
            return data[coin]['USD']
        
    except:
        print("Unexpected error:", sys.exc_info())

    return