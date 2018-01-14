Students:
Jakob Zanker/ 803031
Aviv Ples/ 318357233

Chunk:
Holding a byte array of data. We read a chunk from the Http stream in the RangeGetter and write it to the File in the FileWriter

DownloadableMetadata:
Holds information of the downloaded file, like size, filename etc. Holds the ranges of data that are already downloaded.
Is persisted on the filesystem, so that the download can be resumed.

DownloadStatus:
Prints the percentage of bytes already downloaded.

FileWriter:
Takes Chunks of data out of a queue and writes in on the filesystem.

HTTPRangeGetter:
Opens a Http connection and requests a Range of data using a Range Get. Reads from the stream chunks of data in puts them
into the Queue

IdcDm:
Contains the main method of the program. Optionally receives the number of download workers and a bandwith limit as
parameters. Sets up all the required objects and starts a download for the given url.

Range:
A range of data with a start, end and length.

RateLimiter:
Runnable that adds token to the Tokenbucket every second. Implements a soft and a hard limit.

TokenBucket:
A bucket containing tokens. The RangeGetter checks if the bucket contains the required amount of tokens, if yes it continues
to read from the stream, if no it blocks and waits.