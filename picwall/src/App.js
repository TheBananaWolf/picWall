import React, { useState, useEffect } from "react";

function App() {
  const url = "http://127.0.0.1:8080/showmedia"; // Changed to a more general endpoint name
  const [mediaSrc, setMediaSrc] = useState(null);
  const [mediaType, setMediaType] = useState(null);

  useEffect(() => {
    const fetchMedia = () => {
      fetch(url)
        .then((response) => {
          if (!response.ok) {
            throw new Error("Network response was not ok");
          }
          const contentType = response.headers.get("content-type");
          if (contentType.startsWith("video")) {
            setMediaType("video");
          } else if (contentType.startsWith("image/gif")) {
            setMediaType("gif");
          } else if (contentType.startsWith("image")) {
            setMediaType("image");
          }
          return response.blob();
        })
        .then((mediaBlob) => {
          const mediaObjectURL = URL.createObjectURL(mediaBlob);
          setMediaSrc(mediaObjectURL);
        })
        .catch((error) => {
          console.error("Error fetching the media:", error);
          setMediaSrc(null);
          setMediaType(null);
        });
    };

    fetchMedia();
    const intervalId = setInterval(fetchMedia, 8000); // Interval can be adjusted based on your needs

    return () => clearInterval(intervalId);
  }, [url]);

  const renderMedia = () => {
    switch (mediaType) {
      case "video":
        return (
          <video
            src={mediaSrc}
            controls
            autoPlay
            loop
            muted
            style={{ maxHeight: "100%", maxWidth: "100%" }}
          />
        );
      case "gif":
      case "image":
        return (
          <img
            src={mediaSrc}
            alt="Dynamic media from server"
            style={{ maxHeight: "100%", maxWidth: "100%" }}
          />
        );
      default:
        return <p>Loading...</p>;
    }
  };

  return (
    <div
      className="App"
      style={{ width: "100%", height: 900, textAlign: "center" }}
    >
      {renderMedia()}
    </div>
  );
}

export default App;