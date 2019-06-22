
import comtypes.client
import os
import sys, getopt

def init_powerpoint():
    powerpoint = comtypes.client.CreateObject("Powerpoint.Application")
    powerpoint.Visible = 1
    return powerpoint


def ppt_to_pdf(powerpoint, inputFileName, outputFileName, formatType=32):
    deck = powerpoint.Presentations.Open(inputFileName)
    deck.SaveAs(outputFileName, formatType)  # formatType = 32 for ppt to pdf
    deck.Close()

if __name__ == "__main__":
    source=""
    target=""
    opts, args = getopt.getopt(sys.argv[1:], "s:t:")
    if opts:
        for k, v in opts:
            if "-s" == k:
                source=v
            if "-t" == k:
                target=v
    powerpoint = init_powerpoint()
    print (source+"--"+target)
    ppt_to_pdf(powerpoint,source,target)
    powerpoint.Quit()


